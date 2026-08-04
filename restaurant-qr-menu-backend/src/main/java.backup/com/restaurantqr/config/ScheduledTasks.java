package com.restaurantqr.config;

import com.restaurantqr.modules.subscription.entity.Subscription;
import com.restaurantqr.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final SubscriptionRepository subscriptionRepository;

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
                        com.restaurantqr.modules.restaurant.entity.Restaurant.SubscriptionPlan.BASIC);
                count++;
            }
        }

        if (count > 0) {
            log.info("Expired {} subscriptions and downgraded restaurants to BASIC", count);
        }
    }

    /**
     * Every day at 09:00 AM — log upcoming expirations (hook for email reminders).
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void warnExpiringSubscriptions() {
        List<Subscription> expiringSoon = subscriptionRepository.findExpiringSoon(
                LocalDate.now(), LocalDate.now().plusDays(7));

        if (!expiringSoon.isEmpty()) {
            log.info("{} subscription(s) expiring within 7 days — send renewal emails", expiringSoon.size());
            // TODO: inject EmailService and send renewal reminder
        }
    }
}
