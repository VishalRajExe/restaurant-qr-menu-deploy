package com.restaurantqr.platform.modules.branch.service;

import com.restaurantqr.platform.common.ForbiddenException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
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
    private final com.restaurantqr.platform.audit.service.AuditLogService auditLogService;

    public List<Branch> findByRestaurant(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return branchRepository.findByRestaurantId(restaurantId);
    }

    public Branch findById(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var branch = branchRepository.findById(id)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        assertOwnership(branch, restaurantId);
        return branch;
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

        Branch saved = branchRepository.save(branch);
        auditLogService.log(restaurantId, "BRANCH_CREATED", "Branch", saved.getId(), null, saved.getName());
        return saved;
    }

    @Transactional
    public Branch update(Long id, Long restaurantId, BranchRequest request) {
        var branch = findById(id, restaurantId);
        String oldName = branch.getName();

        branch.setName(request.name);
        branch.setAddress(request.address);
        branch.setPhone(request.phone);
        branch.setOpeningHours(request.openingHours);
        branch.setLatitude(request.latitude);
        branch.setLongitude(request.longitude);

        Branch updated = branchRepository.save(branch);
        auditLogService.log(restaurantId, "BRANCH_UPDATED", "Branch", updated.getId(), oldName, updated.getName());
        return updated;
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var branch = findById(id, restaurantId);
        branch.softDelete();
        branchRepository.save(branch);
        auditLogService.log(restaurantId, "BRANCH_DELETED", "Branch", branch.getId(), branch.getName(), "DELETED");
    }

    @Transactional
    public Branch restore(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        assertOwnership(branch, restaurantId);
        branch.restore();
        Branch restored = branchRepository.save(branch);
        auditLogService.log(restaurantId, "BRANCH_RESTORED", "Branch", restored.getId(), "DELETED", restored.getName());
        return restored;
    }

    private void assertOwnership(Branch branch, Long restaurantId) {
        if (!branch.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This branch does not belong to your restaurant");
        }
    }
}



