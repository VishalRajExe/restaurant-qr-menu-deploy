package com.restaurantqr.platform.modules.category.service;

import com.restaurantqr.platform.common.ForbiddenException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final RestaurantService restaurantService;
    private final com.restaurantqr.platform.audit.service.AuditLogService auditLogService;

    public List<Category> findByRestaurant(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return categoryRepository.findByRestaurantIdOrdered(restaurantId);
    }

    public List<Category> findActiveByRestaurant(Long restaurantId) {
        return categoryRepository.findActiveByRestaurantId(restaurantId);
    }

    public Category findById(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var category = categoryRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        assertOwnership(category, restaurantId);
        return category;
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public Category create(Long restaurantId, CategoryRequest request) {
        var restaurant = restaurantService.findById(restaurantId);

        var category = Category.builder()
                .restaurant(restaurant)
                .name(request.name)
                .description(request.description)
                .displayOrder(request.displayOrder != null ? request.displayOrder : 0)
                .build();

        Category saved = categoryRepository.save(category);
        auditLogService.log(restaurantId, "CATEGORY_CREATED", "Category", saved.getId(), null, saved.getName());
        return saved;
    }

    @Transactional
    public Category update(Long id, Long restaurantId, CategoryRequest request) {
        var category = findById(id, restaurantId);
        String oldName = category.getName();

        category.setName(request.name);
        category.setDescription(request.description);
        if (request.displayOrder != null) category.setDisplayOrder(request.displayOrder);

        Category updated = categoryRepository.save(category);
        auditLogService.log(restaurantId, "CATEGORY_UPDATED", "Category", updated.getId(), oldName, updated.getName());
        return updated;
    }

    @Transactional
    public void updateImage(Long id, Long restaurantId, String imageUrl) {
        var category = findById(id, restaurantId);
        category.setImageUrl(imageUrl);
        categoryRepository.save(category);
    }

    /**
     * Drag-and-drop reorder: accepts [{id, displayOrder}, ...] pairs
     * and updates them all in one transaction.
     */
    @Transactional
    public void reorder(Long restaurantId, List<ReorderItem> items) {
        restaurantService.findById(restaurantId);
        for (ReorderItem item : items) {
            var category = findById(item.id, restaurantId);
            categoryRepository.updateDisplayOrder(item.id, item.displayOrder);
        }
        log.info("Reordered {} categories for restaurant={}", items.size(), restaurantId);
    }

    @Transactional
    public void toggleStatus(Long id, Long restaurantId) {
        var category = findById(id, restaurantId);
        category.setStatus(category.getStatus() == Category.Status.ACTIVE
                ? Category.Status.INACTIVE : Category.Status.ACTIVE);
        categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var category = findById(id, restaurantId);
        category.softDelete();
        categoryRepository.save(category);
        auditLogService.log(restaurantId, "CATEGORY_DELETED", "Category", category.getId(), category.getName(), "DELETED");
    }

    @Transactional
    public Category restore(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        assertOwnership(category, restaurantId);
        category.restore();
        Category restored = categoryRepository.save(category);
        auditLogService.log(restaurantId, "CATEGORY_RESTORED", "Category", restored.getId(), "DELETED", restored.getName());
        return restored;
    }

    private void assertOwnership(Category category, Long restaurantId) {
        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This category does not belong to your restaurant");
        }
    }
}

