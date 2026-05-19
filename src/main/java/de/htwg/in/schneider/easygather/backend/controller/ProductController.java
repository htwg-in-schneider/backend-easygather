package de.htwg.in.schneider.easygather.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    public static class Product {
        private String title;
        private String description;

        public Product() {
        }

        public Product(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @GetMapping
    public List<Product> getProducts() {
        return Arrays.asList(
                new Product("Date-Korb", "Kleine, stilvolle Auswahl für zwei Personen."),
                new Product("Standard-Korb", "Der flexible Allrounder für Treffen im Park."),
                new Product("Familien-Korb", "Großer Mix für Groß und Klein."),
                new Product("LED-Lichterkette", "Stimmungsvolle Lichterkette für Abende im Freien."),
                new Product("Geburtstags-Deko Paket", "Tischdeko und Banner für kleine Feiern."),
                new Product("Pizza Margherita (groß)", "Frische Pizza zum Teilen."),
                new Product("Hausgemachte Zitronenlimonade", "Erfrischend und nicht zu süß."));
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody Product product) {
        System.out.println(
                "Controller called for product: " + product.getTitle() + " - " + product.getDescription());
        return ResponseEntity.ok("POST successful");
    }
}
