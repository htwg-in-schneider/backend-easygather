package de.htwg.in.schneider.easygather.backend.dto;

public class CategorySummary {

    private Long id;
    private String title;
    private String shopCategory;
    private long productCount;

    public CategorySummary(Long id, String title, String shopCategory, long productCount) {
        this.id = id;
        this.title = title;
        this.shopCategory = shopCategory;
        this.productCount = productCount;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public long getProductCount() {
        return productCount;
    }
}
