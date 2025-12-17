package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.ReservationDetail;

public interface ReservationDetailRepo extends JpaRepository<ReservationDetail, Long> {
}
