package de.htwg.in.schneider.easygather.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.model.Product;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @GetMapping
    public List<Product> getProducts() {
        Product dateKorb = new Product();
        dateKorb.setId(1);
        dateKorb.setTitle("Date-Korb");
        dateKorb.setDescription("Kleine, stilvolle Auswahl für zwei Personen – ideal für ein entspanntes Date im Park.");
        dateKorb.setCategory(Category.PICNIC_BASKETS);
        dateKorb.setPrice(45.00);
        dateKorb.setImageUrl("https://picsum.photos/seed/easygather-datekorb/640/480");

        Product standardKorb = new Product();
        standardKorb.setId(2);
        standardKorb.setTitle("Standard-Korb");
        standardKorb.setDescription("Der flexible Allrounder für Treffen im Park mit Freunden oder Kolleg:innen.");
        standardKorb.setCategory(Category.PICNIC_BASKETS);
        standardKorb.setPrice(65.00);
        standardKorb.setImageUrl("https://picsum.photos/seed/easygather-standardkorb/640/480");

        Product familienKorb = new Product();
        familienKorb.setId(3);
        familienKorb.setTitle("Familien-Korb");
        familienKorb.setDescription("Großer Mix für Groß und Klein – Snacks, Getränke und kleine Überraschungen inklusive.");
        familienKorb.setCategory(Category.PICNIC_BASKETS);
        familienKorb.setPrice(95.00);
        familienKorb.setImageUrl("https://picsum.photos/seed/easygather-familienkorb/640/480");

        Product ledLichterkette = new Product();
        ledLichterkette.setId(4);
        ledLichterkette.setTitle("LED-Lichterkette");
        ledLichterkette.setDescription("Stimmungsvolle Lichterkette für Abende im Freien – batteriebetrieben und wetterfest.");
        ledLichterkette.setCategory(Category.PARTY_EVENT);
        ledLichterkette.setPrice(24.99);
        ledLichterkette.setImageUrl("https://picsum.photos/seed/easygather-led/640/480");

        Product geburtstagsDeko = new Product();
        geburtstagsDeko.setId(5);
        geburtstagsDeko.setTitle("Geburtstags-Deko Paket");
        geburtstagsDeko.setDescription("Tischdeko, Banner und Accessoires für kleine Feiern im Freien oder zu Hause.");
        geburtstagsDeko.setCategory(Category.PARTY_EVENT);
        geburtstagsDeko.setPrice(35.00);
        geburtstagsDeko.setImageUrl("https://picsum.photos/seed/easygather-deko/640/480");

        Product pizza = new Product();
        pizza.setId(6);
        pizza.setTitle("Pizza Margherita (groß)");
        pizza.setDescription("Frische Pizza zum Teilen – perfekt als Ergänzung zu Picknick und Party.");
        pizza.setCategory(Category.FOOD_AND_DRINKS);
        pizza.setPrice(12.50);
        pizza.setImageUrl("https://picsum.photos/seed/easygather-pizza/640/480");

        Product limonade = new Product();
        limonade.setId(7);
        limonade.setTitle("Hausgemachte Zitronenlimonade");
        limonade.setDescription("Erfrischend und nicht zu süß – inklusive Mehrwegflaschen für nachhaltige Events.");
        limonade.setCategory(Category.FOOD_AND_DRINKS);
        limonade.setPrice(8.50);
        limonade.setImageUrl("https://picsum.photos/seed/easygather-limonade/640/480");

        return Arrays.asList(dateKorb, standardKorb, familienKorb, ledLichterkette, geburtstagsDeko, pizza, limonade);
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody Product product) {
        System.out.println(
                "Controller called for product: " + product.getTitle() + " - " + product.getDescription());
        return ResponseEntity.ok("POST successful");
    }
}
