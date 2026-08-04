package com.restaurantqr.platform.modules.restaurant;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.config.EmailService;
import com.restaurantqr.platform.modules.restaurant.controller.PublicMenuController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RestaurantQrApplication.class)
class ControllerTest {

    @Autowired
    private PublicMenuController publicMenuController;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void controllerShouldBeLoaded() {
        assertThat(publicMenuController).isNotNull();
    }
}