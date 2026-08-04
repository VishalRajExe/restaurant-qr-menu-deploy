package com.restaurantqr.platform.modules.menuitem.service;

import com.restaurantqr.platform.common.*;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
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
    private final com.restaurantqr.platform.audit.service.AuditLogService auditLogService;
    private final com.restaurantqr.platform.modules.menuitem.repository.CustomerFavoriteRepository customerFavoriteRepository;

    // ─── Public menu (no auth) ────────────────────────────────────────────────

    public List<MenuItem> getPublicMenu(Long restaurantId) {
        return menuItemRepository.findActiveByRestaurantId(restaurantId);
    }

    public Page<MenuItem> searchMenu(Long restaurantId, String search,
                                     MenuItem.FoodType foodType, Pageable pageable) {
        return menuItemRepository.searchMenu(restaurantId, search, foodType, pageable);
    }

    public List<MenuItem> searchPublicMenu(Long restaurantId, String search, MenuItem.FoodType foodType) {
        Page<MenuItem> page = menuItemRepository.searchMenu(restaurantId, search, foodType, Pageable.unpaged());
        return page.getContent();
    }

    public List<MenuItem> getRecommended(Long restaurantId) {
        return menuItemRepository.findRecommendedByRestaurantId(restaurantId);
    }

    public List<MenuItem> getRecentlyAdded(Long restaurantId) {
        return menuItemRepository.findRecentlyAddedByRestaurantId(restaurantId);
    }

    public List<MenuItem> getCombos(Long restaurantId) {
        return menuItemRepository.findCombosByRestaurantId(restaurantId);
    }

    public List<MenuItem> getRelatedItems(Long restaurantId, Long itemId) {
        MenuItem item = findByIdAndRestaurant(itemId, restaurantId);
        return menuItemRepository.findRelatedItems(restaurantId, item.getCategory().getId(), itemId);
    }

    @Transactional
    public boolean toggleFavorite(String deviceToken, Long restaurantId, Long menuItemId) {
        var existing = customerFavoriteRepository.findByDeviceTokenAndMenuItemId(deviceToken, menuItemId);
        if (existing.isPresent()) {
            var fav = existing.get();
            if (Boolean.TRUE.equals(fav.getIsDeleted())) {
                fav.setIsDeleted(false);
                customerFavoriteRepository.save(fav);
                return true;
            } else {
                fav.setIsDeleted(true);
                customerFavoriteRepository.save(fav);
                return false;
            }
        } else {
            var restaurant = restaurantService.findById(restaurantId);
            var menuItem = findByIdAndRestaurant(menuItemId, restaurantId);
            var fav = com.restaurantqr.platform.modules.menuitem.entity.CustomerFavorite.builder()
                    .deviceToken(deviceToken)
                    .restaurant(restaurant)
                    .menuItem(menuItem)
                    .build();
            customerFavoriteRepository.save(fav);
            return true;
        }
    }

    public List<MenuItem> getFavorites(String deviceToken, Long restaurantId) {
        return customerFavoriteRepository.findFavoritesByDeviceTokenAndRestaurant(deviceToken, restaurantId);
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
                .vegNonveg(request.vegNonveg != null ? request.vegNonveg : MenuItem.FoodType.NON_VEG)
                .isAvailable(request.isAvailable != null ? request.isAvailable : true)
                .isFeatured(request.isFeatured != null ? request.isFeatured : false)
                .isPopular(request.isPopular != null ? request.isPopular : false)
                .isChefSpecial(request.isChefSpecial != null ? request.isChefSpecial : false)
                .spiceLevel(request.spiceLevel != null ? request.spiceLevel : 0)
                .calories(request.calories)
                .proteinGrams(request.proteinGrams != null ? request.proteinGrams : BigDecimal.ZERO)
                .fatGrams(request.fatGrams != null ? request.fatGrams : BigDecimal.ZERO)
                .carbsGrams(request.carbsGrams != null ? request.carbsGrams : BigDecimal.ZERO)
                .allergens(request.allergens)
                .isVegan(request.isVegan != null ? request.isVegan : false)
                .isHalal(request.isHalal != null ? request.isHalal : false)
                .isJain(request.isJain != null ? request.isJain : false)
                .isGlutenFree(request.isGlutenFree != null ? request.isGlutenFree : false)
                .mealType(request.mealType != null ? request.mealType : MenuItem.MealType.ALL_DAY)
                .isCombo(request.isCombo != null ? request.isCombo : false)
                .comboDescription(request.comboDescription)
                .prepTimeMinutes(request.prepTimeMinutes)
                .displayOrder(request.displayOrder != null ? request.displayOrder : 0)
                .tags(request.tags)
                .build();

        MenuItem saved = menuItemRepository.save(item);
        auditLogService.log(restaurantId, "MENU_ITEM_CREATED", "MenuItem", saved.getId(), null, saved.getName() + " (₹" + saved.getPrice() + ")");
        return saved;
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

        BigDecimal oldPrice = item.getPrice();
        String oldName = item.getName();

        item.setName(request.name);
        item.setDescription(request.description);
        item.setPrice(request.price);
        item.setVegNonveg(request.vegNonveg);
        if (request.isAvailable != null) item.setIsAvailable(request.isAvailable);
        if (request.isFeatured != null) item.setIsFeatured(request.isFeatured);
        if (request.isPopular != null) item.setIsPopular(request.isPopular);
        if (request.isChefSpecial != null) item.setIsChefSpecial(request.isChefSpecial);
        if (request.spiceLevel != null) item.setSpiceLevel(request.spiceLevel);
        item.setCalories(request.calories);
        if (request.proteinGrams != null) item.setProteinGrams(request.proteinGrams);
        if (request.fatGrams != null) item.setFatGrams(request.fatGrams);
        if (request.carbsGrams != null) item.setCarbsGrams(request.carbsGrams);
        item.setAllergens(request.allergens);
        if (request.isVegan != null) item.setIsVegan(request.isVegan);
        if (request.isHalal != null) item.setIsHalal(request.isHalal);
        if (request.isJain != null) item.setIsJain(request.isJain);
        if (request.isGlutenFree != null) item.setIsGlutenFree(request.isGlutenFree);
        if (request.mealType != null) item.setMealType(request.mealType);
        if (request.isCombo != null) item.setIsCombo(request.isCombo);
        item.setComboDescription(request.comboDescription);
        item.setPrepTimeMinutes(request.prepTimeMinutes);
        if (request.displayOrder != null) item.setDisplayOrder(request.displayOrder);
        item.setTags(request.tags);


        MenuItem updated = menuItemRepository.save(item);

        if (oldPrice != null && request.price != null && oldPrice.compareTo(request.price) != 0) {
            auditLogService.log(restaurantId, "ITEM_PRICE_CHANGED", "MenuItem", updated.getId(),
                    "₹" + oldPrice, "₹" + updated.getPrice());
        } else {
            auditLogService.log(restaurantId, "MENU_ITEM_UPDATED", "MenuItem", updated.getId(), oldName, updated.getName());
        }

        return updated;
    }

    @Transactional
    public void updateAvailability(Long id, Long restaurantId, boolean available) {
        var item = findByIdAndRestaurant(id, restaurantId);
        boolean oldAvailability = item.getIsAvailable();
        item.setIsAvailable(available);
        menuItemRepository.save(item);
        auditLogService.log(restaurantId, "ITEM_AVAILABILITY_CHANGED", "MenuItem", item.getId(),
                String.valueOf(oldAvailability), String.valueOf(available));
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
        auditLogService.log(restaurantId, "MENU_ITEM_DELETED", "MenuItem", item.getId(), item.getName(), "DELETED");
    }

    @Transactional
    public MenuItem restore(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
        if (!item.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("This menu item does not belong to your restaurant");
        }
        item.restore();
        MenuItem restored = menuItemRepository.save(item);
        auditLogService.log(restaurantId, "MENU_ITEM_RESTORED", "MenuItem", restored.getId(), "DELETED", restored.getName());
        return restored;
    }

    public List<MenuItem> getByCategory(Long categoryId, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var category = categoryRepository.findById(categoryId)
                .filter(c -> c.getRestaurant().getId().equals(restaurantId) && !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        return menuItemRepository.findActiveByCategoryId(category.getId());
    }

    public List<MenuItem> getFeatured(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return menuItemRepository.findFeaturedByRestaurantId(restaurantId);
    }

    private MenuItem findByIdAndRestaurant(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        return menuItemRepository.findById(id)
                .filter(m -> m.getRestaurant().getId().equals(restaurantId) && !m.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }
}


