package com.restaurantqr.modules.menuitem.service;

import com.restaurantqr.common.*;
import com.restaurantqr.modules.category.repository.CategoryRepository;
import com.restaurantqr.modules.menuitem.entity.MenuItem;
import com.restaurantqr.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantService restaurantService;

    // ─── Public menu (no auth) ────────────────────────────────────────────────

    public List<MenuItem> getPublicMenu(Long restaurantId) {
        return menuItemRepository.findActiveByRestaurantId(restaurantId);
    }

    public Page<MenuItem> searchMenu(Long restaurantId, String search,
                                     MenuItem.FoodType foodType, Pageable pageable) {
        return menuItemRepository.searchMenu(restaurantId, search, foodType, pageable);
    }

    // ─── Admin CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public MenuItem create(Long restaurantId, MenuItemRequest request) {
        restaurantService.assertMenuItemLimit(restaurantId);

        var restaurant = restaurantService.findById(restaurantId);
        var category = categoryRepository.findById(request.categoryId)
                .filter(c -> c.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId));

        var item = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.name)
                .description(request.description)
                .price(request.price)
                .vegNonveg(request.vegNonveg)
                .isAvailable(request.isAvailable != null ? request.isAvailable : true)
                .isFeatured(request.isFeatured != null ? request.isFeatured : false)
                .calories(request.calories)
                .prepTimeMinutes(request.prepTimeMinutes)
                .displayOrder(request.displayOrder != null ? request.displayOrder : 0)
                .tags(request.tags)
                .build();

        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem update(Long id, Long restaurantId, MenuItemRequest request) {
        var item = findByIdAndRestaurant(id, restaurantId);

        if (!item.getCategory().getId().equals(request.categoryId)) {
            var category = categoryRepository.findById(request.categoryId)
                    .filter(c -> c.getRestaurant().getId().equals(restaurantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId));
            item.setCategory(category);
        }

        item.setName(request.name);
        item.setDescription(request.description);
        item.setPrice(request.price);
        item.setVegNonveg(request.vegNonveg);
        if (request.isAvailable != null) item.setIsAvailable(request.isAvailable);
        if (request.isFeatured != null) item.setIsFeatured(request.isFeatured);
        item.setCalories(request.calories);
        item.setPrepTimeMinutes(request.prepTimeMinutes);
        if (request.displayOrder != null) item.setDisplayOrder(request.displayOrder);
        item.setTags(request.tags);

        return menuItemRepository.save(item);
    }

    @Transactional
    public void updateAvailability(Long id, Long restaurantId, boolean available) {
        var item = findByIdAndRestaurant(id, restaurantId);
        item.setIsAvailable(available);
        menuItemRepository.save(item);
    }

    @Transactional
    public void updateImageUrl(Long id, Long restaurantId, String imageUrl) {
        var item = findByIdAndRestaurant(id, restaurantId);
        item.setImageUrl(imageUrl);
        menuItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var item = findByIdAndRestaurant(id, restaurantId);
        item.softDelete();
        menuItemRepository.save(item);
    }

    public List<MenuItem> getByCategory(Long categoryId) {
        return menuItemRepository.findActiveByCategoryId(categoryId);
    }

    public List<MenuItem> getFeatured(Long restaurantId) {
        return menuItemRepository.findFeaturedByRestaurantId(restaurantId);
    }

    private MenuItem findByIdAndRestaurant(Long id, Long restaurantId) {
        return menuItemRepository.findById(id)
                .filter(m -> m.getRestaurant().getId().equals(restaurantId) && !m.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }
}

