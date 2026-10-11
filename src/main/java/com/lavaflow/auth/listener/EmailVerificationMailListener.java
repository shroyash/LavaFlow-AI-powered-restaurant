package com.lavaflow.auth.listener;

import com.lavaflow.auth.event.EmailVerificationRequestedEvent;
import com.lavaflow.common.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailVerificationMailListener {

    private final EmailSender emailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.email-verification.expiry-hours:24}")
    private long expiryHours;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationRequested(EmailVerificationRequestedEvent event) {
        String link = frontendBaseUrl + "/verify-email?token=" + event.token();

        String body = "Hello " + event.fullName() + ",\n\n"
                + "Please verify your email address to continue with your LavaFlow registration:\n\n"
                + link + "\n\n"
                + "This link expires in " + expiryHours + " hours. "
                + "If you did not register, you can ignore this email.\n\n"
                + "LavaFlow";

        emailSender.send(event.email(), "Verify your email address", body);
    }
}