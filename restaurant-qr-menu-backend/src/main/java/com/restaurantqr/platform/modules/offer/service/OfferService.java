package com.restaurantqr.platform.modules.offer.service;

import com.restaurantqr.platform.common.ForbiddenException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.repository.OfferRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final RestaurantService restaurantService;

    /** Public endpoint — customer sees live deals */
    public List<Offer> getActiveOffers(Long restaurantId) {
        return offerRepository.findActiveOffers(restaurantId, LocalDate.now());
    }

    public List<Offer> getAllByRestaurant(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return offerRepository.findAllByRestaurantId(restaurantId);
    }

    public Offer findById(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var offer = offerRepository.findById(id)
                .filter(o -> !o.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Offer", id));
        assertOwnership(offer, restaurantId);
        return offer;
    }

    public Offer findById(Long id) {
        return offerRepository.findById(id)
                .filter(o -> !o.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Offer", id));
    }

    @Transactional
    public Offer create(Long restaurantId, OfferRequest request) {
        validateOfferRequest(request);
        var restaurant = restaurantService.findById(restaurantId);

        var offer = Offer.builder()
                .restaurant(restaurant)
                .title(request.title)
                .description(request.description)
                .discountType(request.discountType)
                .discountPercentage(request.discountPercentage)
                .discountAmount(request.discountAmount)
                .startDate(request.startDate)
                .endDate(request.endDate)
                .build();

        return offerRepository.save(offer);
    }

    @Transactional
    public Offer update(Long id, Long restaurantId, OfferRequest request) {
        validateOfferRequest(request);
        var offer = findById(id, restaurantId);

        offer.setTitle(request.title);
        offer.setDescription(request.description);
        offer.setDiscountType(request.discountType);
        offer.setDiscountPercentage(request.discountPercentage);
        offer.setDiscountAmount(request.discountAmount);
        offer.setStartDate(request.startDate);
        offer.setEndDate(request.endDate);

        return offerRepository.save(offer);
    }

    private void validateOfferRequest(OfferRequest request) {
        if (request.discountType == null) {
            if (request.discountPercentage != null) {
                request.discountType = Offer.DiscountType.PERCENTAGE;
            } else if (request.discountAmount != null) {
                request.discountType = Offer.DiscountType.FLAT;
            } else {
                request.discountType = Offer.DiscountType.PERCENTAGE;
            }
        }
        if (request.endDate != null && request.startDate != null && request.endDate.isBefore(request.startDate)) {
            throw new com.restaurantqr.platform.common.BadRequestException("End date cannot be before start date");
        }
        if (request.discountType == Offer.DiscountType.PERCENTAGE) {
            if (request.discountPercentage == null || request.discountPercentage.compareTo(java.math.BigDecimal.ZERO) <= 0 || request.discountPercentage.compareTo(new java.math.BigDecimal("100")) > 0) {
                throw new com.restaurantqr.platform.common.BadRequestException("Percentage discount must be between 0.01 and 100%");
            }
        } else if (request.discountType == Offer.DiscountType.FLAT) {
            if (request.discountAmount == null || request.discountAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new com.restaurantqr.platform.common.BadRequestException("Fixed discount amount must be greater than 0");
            }
        }
    }


    @Transactional
    public void updateBanner(Long id, Long restaurantId, String bannerUrl) {
        var offer = findById(id, restaurantId);
        offer.setBannerUrl(bannerUrl);
        offerRepository.save(offer);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var offer = findById(id, restaurantId);
        offer.softDelete();
        offerRepository.save(offer);
    }

    private void assertOwnership(Offer offer, Long restaurantId) {
        if (!offer.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This offer does not belong to your restaurant");
        }
    }
}
