package com.restaurantqr.modules.branch.service;

import com.restaurantqr.common.ForbiddenException;
import com.restaurantqr.common.ResourceNotFoundException;
import com.restaurantqr.modules.branch.entity.Branch;
import com.restaurantqr.modules.branch.repository.BranchRepository;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final RestaurantService restaurantService;

    public List<Branch> findByRestaurant(Long restaurantId) {
        return branchRepository.findByRestaurantId(restaurantId);
    }

    public Branch findById(Long id) {
        return branchRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    @Transactional
    public Branch create(Long restaurantId, BranchRequest request) {
        restaurantService.assertBranchLimit(restaurantId);
        var restaurant = restaurantService.findById(restaurantId);

        var branch = Branch.builder()
                .restaurant(restaurant)
                .name(request.name)
                .address(request.address)
                .phone(request.phone)
                .openingHours(request.openingHours)
                .latitude(request.latitude)
                .longitude(request.longitude)
                .build();

        return branchRepository.save(branch);
    }

    @Transactional
    public Branch update(Long id, Long restaurantId, BranchRequest request) {
        var branch = findById(id);
        assertOwnership(branch, restaurantId);

        branch.setName(request.name);
        branch.setAddress(request.address);
        branch.setPhone(request.phone);
        branch.setOpeningHours(request.openingHours);
        branch.setLatitude(request.latitude);
        branch.setLongitude(request.longitude);

        return branchRepository.save(branch);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var branch = findById(id);
        assertOwnership(branch, restaurantId);
        branch.softDelete();
        branchRepository.save(branch);
        log.info("Branch soft-deleted: id={} restaurantId={}", id, restaurantId);
    }

    private void assertOwnership(Branch branch, Long restaurantId) {
        if (!branch.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This branch does not belong to your restaurant");
        }
    }
}


