package pe.edu.vallegrande.sistventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.vallegrande.sistventas.dto.Reservation;

import java.util.List;

public interface ReservationRepo extends JpaRepository<Reservation, Long> {
    // Method to find reservations by active status
    List<Reservation> findByActive(String active);
}