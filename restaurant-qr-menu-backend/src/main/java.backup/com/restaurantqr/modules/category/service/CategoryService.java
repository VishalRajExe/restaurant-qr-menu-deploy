package com.restaurantqr.modules.category.service;

import com.restaurantqr.common.ForbiddenException;
import com.restaurantqr.common.ResourceNotFoundException;
import com.restaurantqr.modules.category.entity.Category;
import com.restaurantqr.modules.category.repository.CategoryRepository;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
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

    public List<Category> findByRestaurant(Long restaurantId) {
        return categoryRepository.findByRestaurantIdOrdered(restaurantId);
    }

    public List<Category> findActiveByRestaurant(Long restaurantId) {
        return categoryRepository.findActiveByRestaurantId(restaurantId);
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

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Long restaurantId, CategoryRequest request) {
        var category = findById(id);
        assertOwnership(category, restaurantId);

        category.setName(request.name);
        category.setDescription(request.description);
        if (request.displayOrder != null) category.setDisplayOrder(request.displayOrder);

        return categoryRepository.save(category);
    }

    @Transactional
    public void updateImage(Long id, Long restaurantId, String imageUrl) {
        var category = findById(id);
        assertOwnership(category, restaurantId);
        category.setImageUrl(imageUrl);
        categoryRepository.save(category);
    }

    /**
     * Drag-and-drop reorder: accepts [{id, displayOrder}, ...] pairs
     * and updates them all in one transaction.
     */
    @Transactional
    public void reorder(Long restaurantId, List<ReorderItem> items) {
        for (ReorderItem item : items) {
            var category = findById(item.id);
            assertOwnership(category, restaurantId);
            categoryRepository.updateDisplayOrder(item.id, item.displayOrder);
        }
        log.info("Reordered {} categories for restaurant={}", items.size(), restaurantId);
    }

    @Transactional
    public void toggleStatus(Long id, Long restaurantId) {
        var category = findById(id);
        assertOwnership(category, restaurantId);
        category.setStatus(category.getStatus() == Category.Status.ACTIVE
                ? Category.Status.INACTIVE : Category.Status.ACTIVE);
        categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var category = findById(id);
        assertOwnership(category, restaurantId);
        category.softDelete();
        categoryRepository.save(category);
    }

    private void assertOwnership(Category category, Long restaurantId) {
        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This category does not belong to your restaurant");
        }
    }
}
