package de.htwg.in.schneider.easygather.backend.dto;

public class CategoryRequest {

    private String title;
    private String shopCategory;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String shopCategory) {
        this.shopCategory = shopCategory;
    }
}
