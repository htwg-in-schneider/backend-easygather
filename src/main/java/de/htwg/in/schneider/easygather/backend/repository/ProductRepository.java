package de.htwg.in.schneider.easygather.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.htwg.in.schneider.easygather.backend.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE "
            + "(:name IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :name, '%'))) AND "
            + "(:categoryId IS NULL OR p.category.id = :categoryId) AND "
            + "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    List<Product> searchProducts(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("maxPrice") Double maxPrice);
}
