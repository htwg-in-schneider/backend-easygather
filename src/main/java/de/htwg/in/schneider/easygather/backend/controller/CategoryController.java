package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.dto.CategoryRequest;
import de.htwg.in.schneider.easygather.backend.dto.CategorySummary;
import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.repository.CategoryRepository;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;
import de.htwg.in.schneider.easygather.backend.util.AdminAuth;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<CategorySummary> getAllCategories(@RequestParam(required = false) String title) {
        LOG.info("Fetching categories, search title={}", title);
        String search = normalizeSearch(title);
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .filter(category -> matchesSearch(category, search))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/translation")
    public java.util.Map<String, String> getCategoryTranslations() {
        LOG.info("Fetching category translations");
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getShopCategory, Category::getTitle));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorySummary> getCategoryById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> ResponseEntity.ok(toSummary(category)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@AuthenticationPrincipal Jwt jwt, @RequestBody CategoryRequest request) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isValidRequest(request)) {
            return ResponseEntity.badRequest().body("Title and shop category are required");
        }
        String shopCategory = request.getShopCategory().trim();
        if (categoryRepository.findByShopCategory(shopCategory).isPresent()) {
            return ResponseEntity.badRequest().body("Shop category already exists");
        }
        Category category = new Category();
        category.setTitle(request.getTitle().trim());
        category.setShopCategory(shopCategory);
        category.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        Category saved = categoryRepository.save(category);
        LOG.info("Created category id={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isValidRequest(request)) {
            return ResponseEntity.badRequest().body("Title and shop category are required");
        }
        String shopCategory = request.getShopCategory().trim();
        Optional<Category> existingKey = categoryRepository.findByShopCategory(shopCategory);
        if (existingKey.isPresent() && !existingKey.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body("Shop category already exists");
        }
        Category category = opt.get();
        category.setTitle(request.getTitle().trim());
        category.setShopCategory(shopCategory);
        category.setImageUrl(normalizeImageUrl(request.getImageUrl()));
        Category saved = categoryRepository.save(category);
        LOG.info("Updated category id={}", saved.getId());
        return ResponseEntity.ok(toSummary(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!AdminAuth.isAdmin(jwt, userRepository)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Category category = opt.get();
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            productRepository.deleteAll(productRepository.findByCategoryId(id));
            LOG.info("Deleted {} products in category id={}", productCount, id);
        }
        categoryRepository.delete(category);
        LOG.info("Deleted category id={}", id);
        return ResponseEntity.noContent().build();
    }

    private CategorySummary toSummary(Category category) {
        long productCount = productRepository.countByCategoryId(category.getId());
        return new CategorySummary(category.getId(), category.getTitle(), category.getShopCategory(),
                productCount, category.getImageUrl());
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    private boolean matchesSearch(Category category, String search) {
        if (search == null) {
            return true;
        }
        String lower = search.toLowerCase(Locale.ROOT);
        return category.getTitle().toLowerCase(Locale.ROOT).contains(lower)
                || category.getShopCategory().toLowerCase(Locale.ROOT).contains(lower);
    }

    private String normalizeSearch(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return title.trim();
    }

    private boolean isValidRequest(CategoryRequest request) {
        return request != null
                && request.getTitle() != null && !request.getTitle().isBlank()
                && request.getShopCategory() != null && !request.getShopCategory().isBlank();
    }
}
