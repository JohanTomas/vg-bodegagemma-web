package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.SaleDetail;

public interface SaleDetailRepo extends JpaRepository<SaleDetail, Long> {
}
