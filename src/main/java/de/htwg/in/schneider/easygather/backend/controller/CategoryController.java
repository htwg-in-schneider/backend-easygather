package de.htwg.in.schneider.easygather.backend.controller;

import java.util.List;

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
}
