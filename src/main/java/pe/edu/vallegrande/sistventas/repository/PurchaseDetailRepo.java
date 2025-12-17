package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.PurchaseDetail;

public interface PurchaseDetailRepo extends JpaRepository<PurchaseDetail, Long> {
}
