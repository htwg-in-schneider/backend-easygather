package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Optional;

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

import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.model.Product;
import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.CategoryRepository;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private boolean userFromJwtIsAdmin(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            LOG.warn("JWT or subject is null");
            return false;
        }
        Optional<User> user = userRepository.findByOauthId(jwt.getSubject());
        if (!user.isPresent() || user.get().getRole() != Role.ADMIN) {
            LOG.warn("Unauthorized product mutation by {}",
                    user.map(u -> "user with oauthId " + u.getOauthId()).orElse("unknown user"));
            return false;
        }
        return true;
    }

    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String shopCategory,
            @RequestParam(required = false) Double maxPrice) {
        Long resolvedCategoryId = resolveCategoryId(categoryId, shopCategory);
        if (resolvedCategoryId == null && shopCategory != null && !shopCategory.isBlank()) {
            LOG.warn("Unknown shopCategory for filter: {}", shopCategory);
            return List.of();
        }
        String searchName = normalizeSearchName(name);
        if (searchName == null && resolvedCategoryId == null && maxPrice == null) {
            return productRepository.findAll();
        }
        LOG.info("Searching products: name={}, categoryId={}, maxPrice={}", searchName, resolvedCategoryId, maxPrice);
        List<Product> products = productRepository.searchProducts(searchName, resolvedCategoryId, maxPrice);
        LOG.info("Found {} products", products.size());
        return products;
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Long categoryId) {
        LOG.info("Fetching products for category id {}", categoryId);
        List<Product> products = productRepository.findByCategoryId(categoryId);
        LOG.info("Found {} products for category {}", products.size(), categoryId);
        return products;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@AuthenticationPrincipal Jwt jwt, @RequestBody Product product) {
        if (!userFromJwtIsAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (product == null) {
            return ResponseEntity.badRequest().build();
        }
        if (product.getId() != null) {
            product.setId(null);
            LOG.warn(
                    "Attempted to create a product with an existing ID. ID has been set to null to create a new product.");
        }
        ResponseEntity<Product> categoryError = resolveCategory(product);
        if (categoryError != null) {
            return categoryError;
        }
        Product newProduct = productRepository.save(product);
        LOG.info("Created new product with id {}", newProduct.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestBody Product productDetails) {
        if (!userFromJwtIsAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Product> opt = productRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Product product = opt.get();
        if (productDetails.getCategory() != null) {
            ResponseEntity<Product> categoryError = resolveCategory(productDetails);
            if (categoryError != null) {
                return categoryError;
            }
            product.setCategory(productDetails.getCategory());
        }
        product.setDescription(productDetails.getDescription());
        product.setImageUrl(productDetails.getImageUrl());
        product.setPrice(productDetails.getPrice());
        product.setTitle(productDetails.getTitle());
        Product updatedProduct = productRepository.save(product);
        LOG.info("Updated product with id {}", updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!userFromJwtIsAdmin(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<Product> opt = productRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(opt.get());
        LOG.info("Deleted product with id {}", id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveCategoryId(Long categoryId, String shopCategory) {
        if (categoryId != null) {
            return categoryId;
        }
        if (shopCategory == null || shopCategory.isBlank()) {
            return null;
        }
        return categoryRepository.findByShopCategory(shopCategory).map(Category::getId).orElse(null);
    }

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private ResponseEntity<Product> resolveCategory(Product product) {
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            LOG.warn("Product category is null or has no id");
            return ResponseEntity.badRequest().build();
        }
        Optional<Category> category = categoryRepository.findById(product.getCategory().getId());
        if (!category.isPresent()) {
            LOG.warn("Category not found for product: {}", product.getCategory().getId());
            return ResponseEntity.badRequest().build();
        }
        product.setCategory(category.get());
        return null;
    }
}
