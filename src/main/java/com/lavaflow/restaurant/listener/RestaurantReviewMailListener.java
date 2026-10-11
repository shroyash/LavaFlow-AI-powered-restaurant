package com.lavaflow.restaurant.listener;

import com.lavaflow.common.email.EmailSender;
import com.lavaflow.restaurant.event.RestaurantReviewedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RestaurantReviewMailListener {

    private final EmailSender emailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRestaurantReviewed(RestaurantReviewedEvent event) {
        if (event.approved()) {
            String body = "Hello " + event.adminName() + ",\n\n"
                    + "Your restaurant \"" + event.restaurantName() + "\" has been approved. "
                    + "You can now log in and start setting up:\n\n"
                    + frontendBaseUrl + "/staff/login\n\n"
                    + "LavaFlow";
            emailSender.send(event.adminEmail(), "Your restaurant has been approved", body);
            return;
        }

        String body = "Hello " + event.adminName() + ",\n\n"
                + "We could not approve your restaurant \"" + event.restaurantName() + "\".\n\n"
                + "Reason: " + event.rejectionReason() + "\n\n"
                + "Please contact support if you have questions.\n\n"
                + "LavaFlow";
        emailSender.send(event.adminEmail(), "Update on your restaurant registration", body);
    }
}