package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import pe.edu.vallegrande.sistventas.dto.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import pe.edu.vallegrande.sistventas.dto.ReservationDetail;
import pe.edu.vallegrande.sistventas.model.Product;
import pe.edu.vallegrande.sistventas.repository.ProductRepo;
import pe.edu.vallegrande.sistventas.repository.ReservationDetailRepo;
import pe.edu.vallegrande.sistventas.repository.ReservationRepo;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationService {
    @Autowired
    private ReservationRepo reservationRepo;

    @Autowired
    private ReservationDetailRepo reservationDetailRepo;

    @Autowired
    private ProductRepo productRepo;

    // Method to get all reservations
    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = reservationRepo.findAll();
        setTransientFields(reservations);
        return reservations;
    }

    // Method to get reservations by active status
    public List<Reservation> getReservationsByStatus(String active) {
        List<Reservation> reservations = reservationRepo.findByActive(active).stream()
                .sorted(Comparator.comparing(Reservation::getId).reversed())
                .collect(Collectors.toList());
        setTransientFields(reservations);
        return reservations;
    }

    // Method to get reservations by active status with pagination
    public Page<Reservation> getReservationsPageableByStatus(String active, Pageable pageable) {
        List<Reservation> allActiveReservations = reservationRepo.findByActive(active);
        allActiveReservations.sort(Comparator.comparing(Reservation::getId).reversed());

        List<Reservation> formattedReservations = allActiveReservations.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        setTransientFields(formattedReservations);
        return new PageImpl<>(formattedReservations, pageable, allActiveReservations.size());
    }

    // Method to get a reservation by its ID
    public Optional<Reservation> getReservationById(Long id) {
        Optional<Reservation> reservation = reservationRepo.findById(id);
        reservation.ifPresent(this::setTransientFields);
        return reservation;
    }

    // Method to create a new reservation
    public Reservation createReservation(Reservation reservation) {
        reservation.setActive("A");
        // Calculate the subtotals and total of the reservation
        calculateReservationTotals(reservation);
        // Set bidirectional relationship
        if (reservation.getReservationDetails() != null) {
            for (ReservationDetail detail : reservation.getReservationDetails()) {
                detail.setReservation(reservation);
            }
        }
        Reservation savedReservation = reservationRepo.save(reservation);
        setTransientFields(savedReservation);
        return savedReservation;
    }

    // Method to delete a reservation
    public void deleteReservation(Long id) {
        reservationRepo.deleteById(id);
    }

    // Method to logically delete a reservation (change status to 'canceled')
    public Reservation logicalCancelReservation(Long id) {
        return reservationRepo.findById(id)
                .map(reservation -> {
                    if ("I".equals(reservation.getActive())) {
                        throw new ResourceConflictException("Reservation with id " + id + " is already inactive");
                    }
                    reservation.setActive("I");
                    return reservationRepo.save(reservation);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id " + id));
    }

    // Method to logically activate a reservation (change status to 'confirmed')
    public Reservation logicalConfirmReservation(Long id) {
        return reservationRepo.findById(id)
                .map(reservation -> {
                    if ("A".equals(reservation.getActive())) {
                        throw new ResourceConflictException("Reservation with id " + id + " is already active");
                    }
                    reservation.setActive("A");
                    return reservationRepo.save(reservation);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id " + id));
    }

    // Method to update a reservation
    public Reservation updateReservation(Long id, Reservation reservationUpdated) {
        Reservation reservation = reservationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id " + id));
        // Update reservation data
        reservation.setClient(reservationUpdated.getClient());
        reservation.setSeller(reservationUpdated.getSeller());
        reservation.setPaymentMethod(reservationUpdated.getPaymentMethod());
        // Set the reservation date to the current server date if no value is provided
        if (reservationUpdated.getReservationDate() == null) {
            reservation.setReservationDate(new Date()); // Current server date
        } else {
            reservation.setReservationDate(reservationUpdated.getReservationDate());
        }
        // Handle reservation details (ReservationDetails)
        updateReservationDetails(reservation, reservationUpdated.getReservationDetails());
        // Calculate the subtotals and total of the reservation
        calculateReservationTotals(reservation);
        // Save the updated reservation
        Reservation savedReservation = reservationRepo.save(reservation);
        setTransientFields(savedReservation);
        return savedReservation;
    }

    // Method to update reservation details
    private void updateReservationDetails(Reservation reservation, List<ReservationDetail> updatedDetails) {
        // Map of current details for quick lookup by ID
        Map<Long, ReservationDetail> currentDetailsMap = reservation.getReservationDetails().stream()
                .collect(Collectors.toMap(ReservationDetail::getId, detail -> detail));
        // Process updated details
        for (ReservationDetail detail : updatedDetails) {
            if (detail.getId() == null) {
                // New detail, add to reservation
                detail.setReservation(reservation);
                reservation.getReservationDetails().add(detail);
            } else if (currentDetailsMap.containsKey(detail.getId())) {
                // Existing detail, update
                ReservationDetail existingDetail = currentDetailsMap.get(detail.getId());
                existingDetail.setProduct(detail.getProduct());
                existingDetail.setAmount(detail.getAmount());
                // No need to remove it from the map because it is not a detail we are going to delete
            }
        }
        // Remove details that are not in the updated details
        reservation.getReservationDetails().removeIf(detail -> !updatedDetails.stream()
                .map(ReservationDetail::getId)
                .collect(Collectors.toList())
                .contains(detail.getId()));
    }

    // Method to calculate the subtotals and total of the reservation
    private void calculateReservationTotals(Reservation reservation) {
        double total = 0;
        for (ReservationDetail detail : reservation.getReservationDetails()) {
            Product product = productRepo.findById(detail.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + detail.getProduct().getId()));
            double subtotal = product.getPriceUnit() * detail.getAmount();
            detail.setSubtotalReservation(subtotal);
            total += subtotal;
        }
        reservation.setTotalReservation(total);
    }

    // Method to set transient fields
    private void setTransientFields(List<Reservation> reservations) {
        for (Reservation reservation : reservations) {
            setTransientFields(reservation);
        }
    }

    private void setTransientFields(Reservation reservation) {
        if (reservation.getClient() != null) {
            reservation.setClientNames(reservation.getClient().getNames() + " " + reservation.getClient().getLastName());
        }
        if (reservation.getSeller() != null) {
            reservation.setSellerNames(reservation.getSeller().getNames() + " " + reservation.getSeller().getLastName());
        }
    }

    // Custom exception for resources not found
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    // Custom exception for resource conflicts
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ResourceConflictException extends RuntimeException {
        public ResourceConflictException(String message) {
            super(message);
        }
    }
}