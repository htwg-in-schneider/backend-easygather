package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.repository.CategoryRepository;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public List<Category> getAllCategories() {
        LOG.info("Fetching all categories");
        List<Category> categories = categoryRepository.findAll();
        LOG.info("Found {} categories", categories.size());
        return categories;
    }

    @GetMapping("/translation")
    public Map<String, String> getCategoryTranslations() {
        LOG.info("Fetching category translations");
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getShopCategory, Category::getTitle));
    }
}
