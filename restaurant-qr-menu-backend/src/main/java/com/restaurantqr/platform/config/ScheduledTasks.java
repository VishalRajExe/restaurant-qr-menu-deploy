package com.restaurantqr.platform.config;

import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;

    /**
     * Every day at 01:00 AM — mark expired subscriptions and downgrade restaurant plan.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository.findExpiringSoon(
                LocalDate.of(2000, 1, 1), LocalDate.now().minusDays(1));

        int count = 0;
        for (Subscription sub : expired) {
            if (sub.getStatus() == Subscription.Status.ACTIVE) {
                sub.setStatus(Subscription.Status.EXPIRED);
                // Downgrade restaurant to BASIC when subscription expires
                var restaurant = sub.getRestaurant();
                restaurant.setSubscriptionPlan(
                        com.restaurantqr.platform.modules.restaurant.entity.Restaurant.SubscriptionPlan.BASIC);
                count++;
            }
        }

        if (count > 0) {
            log.info("Expired {} subscriptions and downgraded restaurants to BASIC", count);
        }
    }

    /**
     * Every day at 09:00 AM — log upcoming expirations and send email reminders.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void warnExpiringSubscriptions() {
        List<Subscription> expiringSoon = subscriptionRepository.findExpiringSoon(
                LocalDate.now(), LocalDate.now().plusDays(7));

        if (!expiringSoon.isEmpty()) {
            log.info("{} subscription(s) expiring within 7 days — sending renewal email reminders", expiringSoon.size());
            for (Subscription sub : expiringSoon) {
                if (sub.getStatus() == Subscription.Status.ACTIVE && sub.getRestaurant() != null) {
                    String targetEmail = sub.getRestaurant().getEmail();
                    if (targetEmail != null && !targetEmail.trim().isEmpty()) {
                        long days = ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate());
                        int daysRemaining = (int) Math.max(0, days);
                        emailService.sendSubscriptionExpirationWarning(
                                targetEmail,
                                sub.getRestaurant().getName(),
                                daysRemaining
                        );
                    }
                }
            }
        }
    }
}
