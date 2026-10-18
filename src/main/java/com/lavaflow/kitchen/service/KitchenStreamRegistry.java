package com.lavaflow.kitchen.service;

import com.lavaflow.kitchen.config.KitchenStreamProperties;
import com.lavaflow.kitchen.dto.KitchenStreamEvent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KitchenStreamRegistry {

    private static final long RECONNECT_MILLIS = 3000;

    private final KitchenStreamProperties properties;
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public KitchenStreamRegistry(KitchenStreamProperties properties) {
        this.properties = properties;
    }

    public Optional<SseEmitter> register(UUID restaurantId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(properties.getTimeoutMinutes()).toMillis());
        boolean[] accepted = {false};

        emitters.compute(restaurantId, (id, clients) -> {
            Set<SseEmitter> set = clients != null ? clients : ConcurrentHashMap.newKeySet();
            if (set.size() < properties.getMaxConnectionsPerRestaurant()) {
                set.add(emitter);
                accepted[0] = true;
            }
            return set.isEmpty() ? null : set;
        });

        if (!accepted[0]) {
            return Optional.empty();
        }

        emitter.onCompletion(() -> remove(restaurantId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> remove(restaurantId, emitter));

        send(restaurantId, emitter, SseEmitter.event()
                .name("CONNECTED")
                .reconnectTime(RECONNECT_MILLIS)
                .data("{\"status\":\"connected\"}", MediaType.APPLICATION_JSON));

        return Optional.of(emitter);
    }

    public void broadcast(UUID restaurantId, KitchenStreamEvent event) {
        Set<SseEmitter> clients = emitters.get(restaurantId);
        if (clients == null) {
            return;
        }

        clients.forEach(emitter -> send(restaurantId, emitter, SseEmitter.event()
                .name(event.type().name())
                .data(event, MediaType.APPLICATION_JSON)));
    }

    public void sendHeartbeat() {
        emitters.forEach((restaurantId, clients) ->
                clients.forEach(emitter -> send(restaurantId, emitter, SseEmitter.event().comment("heartbeat"))));
    }

    public void closeAll(UUID restaurantId) {
        Set<SseEmitter> clients = emitters.remove(restaurantId);
        if (clients == null) {
            return;
        }

        clients.forEach(emitter -> {
            try {
                synchronized (emitter) {
                    emitter.complete();
                }
            } catch (RuntimeException ignored) {
            }
        });
    }

    private void send(UUID restaurantId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            synchronized (emitter) {
                emitter.send(event);
            }
        } catch (IOException | IllegalStateException e) {
            remove(restaurantId, emitter);
        }
    }

    private void remove(UUID restaurantId, SseEmitter emitter) {
        emitters.computeIfPresent(restaurantId, (id, clients) -> {
            clients.remove(emitter);
            return clients.isEmpty() ? null : clients;
        });
    }
}
