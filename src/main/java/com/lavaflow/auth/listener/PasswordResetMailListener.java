package com.lavaflow.auth.listener;

import com.lavaflow.auth.event.PasswordResetRequestedEvent;
import com.lavaflow.common.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {

    private final EmailSender emailSender;

    @Value("${lavaflow.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${lavaflow.password-reset.expiry-minutes:30}")
    private long expiryMinutes;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String link = frontendBaseUrl + "/reset-password?token=" + event.token();

        String body = "Hello " + event.fullName() + ",\n\n"
                + "We received a request to reset your LavaFlow password. Use the link below to choose a new one:\n\n"
                + link + "\n\n"
                + "This link expires in " + expiryMinutes + " minutes and can be used once. "
                + "If you did not ask for this, you can ignore this email and your password will stay the same.\n\n"
                + "LavaFlow";

        emailSender.send(event.email(), "Reset your password", body);
    }
}