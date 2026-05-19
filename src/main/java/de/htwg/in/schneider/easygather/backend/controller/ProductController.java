package de.htwg.in.schneider.easygather.backend.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @GetMapping
    public List<String> getProducts() {
        return Arrays.asList(
                "Date-Korb",
                "Standard-Korb",
                "Familien-Korb",
                "LED-Lichterkette",
                "Geburtstags-Deko Paket",
                "Pizza Margherita (groß)",
                "Hausgemachte Zitronenlimonade");
    }
}
