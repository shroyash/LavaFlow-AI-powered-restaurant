package com.lavaflow.order.service;

import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.order.dto.PlaceOrderRequest;
import com.lavaflow.order.dto.PlaceOrderResult;
import com.lavaflow.order.mapper.OrderMapper;
import com.lavaflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderSubmissionService {

    private static final String IDEMPOTENCY_CONSTRAINT = "uk_orders_customer_idempotency";
    private static final int MIN_KEY_LENGTH = 8;
    private static final int MAX_KEY_LENGTH = 100;

    private final OrderPlacementService orderPlacementService;
    private final OrderRepository orderRepository;
    private final OrderRequestFingerprint orderRequestFingerprint;
    private final OrderMapper orderMapper;

    public PlaceOrderResult submit(UUID customerId, String idempotencyKey, PlaceOrderRequest request) {
        String key = normalizeKey(idempotencyKey);
        String requestHash = orderRequestFingerprint.of(request);

        Optional<PlaceOrderResult> replay = findReplay(customerId, key, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            return new PlaceOrderResult(
                    orderPlacementService.placeOrder(customerId, key, requestHash, request),
                    false
            );
        } catch (DataIntegrityViolationException e) {
            if (!isIdempotencyViolation(e)) {
                throw e;
            }
            return findReplay(customerId, key, requestHash).orElseThrow(() -> e);
        }
    }

    private Optional<PlaceOrderResult> findReplay(UUID customerId, String key, String requestHash) {
        return orderRepository.findByCustomerIdAndIdempotencyKey(customerId, key).map(order -> {
            if (!requestHash.equals(order.getRequestHash())) {
                throw new ConflictException("This idempotency key was already used for a different order.");
            }
            return new PlaceOrderResult(orderMapper.toResponse(order), true);
        });
    }

    private boolean isIdempotencyViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(IDEMPOTENCY_CONSTRAINT);
    }

    private String normalizeKey(String idempotencyKey) {
        String key = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (key.length() < MIN_KEY_LENGTH || key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException(
                    "Idempotency-Key must be between " + MIN_KEY_LENGTH + " and " + MAX_KEY_LENGTH + " characters."
            );
        }
        return key;
    }
}
