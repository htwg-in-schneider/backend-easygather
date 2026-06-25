package de.htwg.in.schneider.easygather.backend.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Double price;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String imageUrl;

    @Lob
    @Column(columnDefinition = "CLOB")
    @JsonIgnore
    private String includedItemsText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @JsonProperty("includedItems")
    public List<String> getIncludedItems() {
        if (includedItemsText == null || includedItemsText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(includedItemsText.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    @JsonProperty("includedItems")
    public void setIncludedItems(List<String> includedItems) {
        if (includedItems == null || includedItems.isEmpty()) {
            includedItemsText = null;
            return;
        }
        includedItemsText = includedItems.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", title='" + title + '\'' + ", description='" + description + '\'' + ", category="
                + category + ", price=" + price + ", imageUrl='" + imageUrl + '\'' + '}';
    }
}
