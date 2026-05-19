package de.htwg.in.schneider.easygather.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.htwg.in.schneider.easygather.backend.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
