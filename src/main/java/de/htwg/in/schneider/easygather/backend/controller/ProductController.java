package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.model.Product;
import de.htwg.in.schneider.easygather.backend.repository.CategoryRepository;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
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
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
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
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
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
    public ResponseEntity<Object> deleteProduct(@PathVariable Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(opt.get());
        LOG.info("Deleted product with id {}", id);
        return ResponseEntity.noContent().build();
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
