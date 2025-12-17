package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.model.Product;
import pe.edu.vallegrande.sistventas.model.Supplier;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepo  extends JpaRepository<Product, Long>{
    Optional<Product> findByCode(String code);
    Optional<Product> findProductByName(String name);
    @Query("SELECT p FROM Product p WHERE lower(p.name) = lower(:name)")
    Optional<Product> findByNameIgnoreCase(@Param("name") String name);
    List<Product> findByActive(String active);
    // Buscar productos por fecha de vencimiento dentro de un rango dado
    List<Product> findByDateExpiryBetween(LocalDate startDate, LocalDate endDate);
    // Buscar productos con stock menor que un valor dado
    List<Product> findByStockLessThan(int stockLimit);
}
