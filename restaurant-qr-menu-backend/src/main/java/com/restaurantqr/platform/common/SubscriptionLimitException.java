package com.restaurantqr.platform.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class SubscriptionLimitException extends RuntimeException {

    public SubscriptionLimitException(String message) {
        super(message);
    }
}