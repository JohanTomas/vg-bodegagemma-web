package pe.edu.vallegrande.sistventas.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.sistventas.model.CategoryProduct;

import java.util.List;
import java.util.Optional;
@Repository
public interface CategoryRepo extends JpaRepository<CategoryProduct, Long>{
    Optional<CategoryProduct> findCategoryProductByName(String name);
    @Query("SELECT c FROM CategoryProduct c WHERE lower(c.name) = lower(:name)")
    Optional<CategoryProduct> findCategoryProductByNameIgnoreCase(@Param("name") String name);
    List<CategoryProduct> findByActive(String active);
}
