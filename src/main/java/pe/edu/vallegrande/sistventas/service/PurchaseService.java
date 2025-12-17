package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import pe.edu.vallegrande.sistventas.dto.Purchase;
import pe.edu.vallegrande.sistventas.dto.PurchaseDetail;
import pe.edu.vallegrande.sistventas.repository.PurchaseDetailRepo;
import pe.edu.vallegrande.sistventas.repository.PurchaseRepo;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepo purchaseRepository;

    @Autowired
    private PurchaseDetailRepo purchaseDetailRepo;

    // Método para obtener todas las compras
    public List<Purchase> getAllPurchases() {
        List<Purchase> purchases = purchaseRepository.findAll();
        setTransientFields(purchases);
        return purchases;
    }

    // Nuevo método para obtener compras por estado
    public List<Purchase> getPurchasesByActiveStatus(String active) {
        List<Purchase> purchases = purchaseRepository.findByActive(active).stream()
                .sorted(Comparator.comparing(Purchase::getId).reversed())
                .collect(Collectors.toList());
        setTransientFields(purchases);
        return purchases;
    }

    // Nuevo método para obtener compras por estado paginador
    public Page<Purchase> getPurchasesPageableByActiveStatus(String active, Pageable pageable) {
        List<Purchase> allActivePurchases = purchaseRepository.findByActive(active);
        allActivePurchases.sort(Comparator.comparing(Purchase::getId).reversed());

        List<Purchase> formattedPurchases = allActivePurchases.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        setTransientFields(formattedPurchases);
        return new PageImpl<>(formattedPurchases, pageable, allActivePurchases.size());
    }

    // Método para obtener una compra por su ID
    public Optional<Purchase> getPurchaseById(Long id) {
        Optional<Purchase> purchase = purchaseRepository.findById(id);
        purchase.ifPresent(this::setTransientFields);
        return purchase;
    }

    // Método para crear una compra
    public Purchase createPurchase(Purchase purchase) {
        purchase.setActive("A");
        // Establecer la relación bidireccional y calcular subtotales
        if (purchase.getPurchaseDetails() != null) {
            double total = 0.0;
            for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
                detail.setPurchase(purchase);
                // Calcular el subtotal del detalle
                detail.setSubtotalPurchase(detail.getPriceUnit() * detail.getAmount());
                total += detail.getSubtotalPurchase();
            }
            // Establecer el total de la compra
            purchase.setTotalPurchase(total);
        }
        Purchase savedPurchase = purchaseRepository.save(purchase);
        setTransientFields(savedPurchase);
        return savedPurchase;
    }

    // Método para eliminar una compra
    public void deletePurchase(Long id) {
        purchaseRepository.deleteById(id);
    }

    // Método para eliminar una compra de manera lógica (cambia el estado a 'I')
    public Purchase logicalDeletPurchase(Long id) {
        return purchaseRepository.findById(id)
                .map(purchase -> {
                    if ("I".equals(purchase.getActive())) {
                        throw new ResourceConflictException("Purchase with id " + id + " is already inactive");
                    }
                    purchase.setActive("I");
                    return purchaseRepository.save(purchase);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id " + id));
    }

    // Método para activar una compra de manera lógica (cambia el estado 'A')
    public Purchase logicalActivarPurchase(Long id) {
        return purchaseRepository.findById(id)
                .map(purchase -> {
                    if ("A".equals(purchase.getActive())) {
                        throw new ResourceConflictException("Purchase with id " + id + " is already active");
                    }
                    purchase.setActive("A");
                    return purchaseRepository.save(purchase);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id " + id));
    }

    // Método para actualizar una compra
    public Purchase updatePurchase(Long id, Purchase purchaseUpdated) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new PurchaseService.ResourceNotFoundException("Purchase not found with id " + id));
        // Actualizar datos de la compra
        purchase.setSupplier(purchaseUpdated.getSupplier());
        purchase.setSeller(purchaseUpdated.getSeller());
        purchase.setPaymentMethod(purchaseUpdated.getPaymentMethod());
        // Establecer el campo dateTime con la fecha actual del servidor si no se proporciona ningún valor
        if (purchaseUpdated.getDateTime() == null) {
            purchase.setDateTime(new Date()); // Fecha actual del servidor
        } else {
            purchase.setDateTime(purchaseUpdated.getDateTime());
        }
        // Manejar los detalles de la compra (PurchaseDetails)
        updatePurchaseDetails(purchase, purchaseUpdated.getPurchaseDetails());
        // Guardar la compra actualizada
        Purchase savedPurchase = purchaseRepository.save(purchase);
        setTransientFields(savedPurchase);
        return savedPurchase;
    }

    private void updatePurchaseDetails(Purchase purchase, List<PurchaseDetail> updatedDetails) {
        // Mapa de detalles actuales para una búsqueda rápida por ID
        Map<Long, PurchaseDetail> currentDetailsMap = purchase.getPurchaseDetails().stream()
                .collect(Collectors.toMap(PurchaseDetail::getId, detail -> detail));
        // Procesar los detalles actualizados
        for (PurchaseDetail detail : updatedDetails) {
            if (detail.getId() == null) {
                // Nuevo detalle, agregar a la compra
                detail.setPurchase(purchase);
                detail.setSubtotalPurchase(detail.getPriceUnit() * detail.getAmount());
                purchase.getPurchaseDetails().add(detail);
            } else if (currentDetailsMap.containsKey(detail.getId())) {
                // Detalle existente, actualizar
                PurchaseDetail existingDetail = currentDetailsMap.get(detail.getId());
                existingDetail.setProduct(detail.getProduct());
                existingDetail.setAmount(detail.getAmount());
                existingDetail.setPriceUnit(detail.getPriceUnit());
                existingDetail.setSubtotalPurchase(detail.getPriceUnit() * detail.getAmount());
                // No es necesario eliminarlo del mapa porque no es un detalle que vamos a eliminar
            }
        }
        // Eliminar los detalles que no están en los detalles actualizados
        purchase.getPurchaseDetails().removeIf(detail -> !updatedDetails.stream()
                .map(PurchaseDetail::getId)
                .collect(Collectors.toList())
                .contains(detail.getId()));
        // Calcular el total de la compra
        double total = purchase.getPurchaseDetails().stream()
                .mapToDouble(PurchaseDetail::getSubtotalPurchase)
                .sum();
        purchase.setTotalPurchase(total);
    }

    // Método para establecer los campos transitorios
    private void setTransientFields(List<Purchase> purchases) {
        for (Purchase purchase : purchases) {
            setTransientFields(purchase);
        }
    }

    private void setTransientFields(Purchase purchase) {
        if (purchase.getSupplier() != null) {
            purchase.setSupplierNames(purchase.getSupplier().getNames() + " " + purchase.getSupplier().getLastName());
        }
        if (purchase.getSeller() != null) {
            purchase.setSellerNames(purchase.getSeller().getNames() + " " + purchase.getSeller().getLastName());
        }
    }

    // Excepción personalizada para recursos no encontrados
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    // Excepción personalizada para conflictos de recursos
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ResourceConflictException extends RuntimeException {
        public ResourceConflictException(String message) {
            super(message);
        }
    }
}
