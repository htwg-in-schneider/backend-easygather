package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.model.Product;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody Product product) {
        System.out.println(
                "Controller called for product: " + product.getTitle() + " - " + product.getDescription());
        return ResponseEntity.ok("POST successful");
    }
}
