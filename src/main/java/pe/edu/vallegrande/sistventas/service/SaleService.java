package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import pe.edu.vallegrande.sistventas.dto.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import pe.edu.vallegrande.sistventas.dto.SaleDetail;
import pe.edu.vallegrande.sistventas.model.Product;
import pe.edu.vallegrande.sistventas.repository.ProductRepo;
import pe.edu.vallegrande.sistventas.repository.SaleDetailRepo;
import pe.edu.vallegrande.sistventas.repository.SaleRepo;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SaleService {
    @Autowired
    private SaleRepo saleRepository;

    @Autowired
    private SaleDetailRepo saleDetailRepo;

    @Autowired
    private ProductRepo productRepo;  // Añadir repositorio de productos

    // Método para obtener todas las ventas
    public List<Sale> getAllSales() {
        List<Sale> sales = saleRepository.findAll();
        setTransientFields(sales);
        return sales;
    }

    // Nuevo método para obtener ventas por estado
    public List<Sale> getSalesByActiveStatus(String active) {
        List<Sale> sales = saleRepository.findByActive(active).stream()
                .sorted(Comparator.comparing(Sale::getId).reversed())
                .collect(Collectors.toList());
        setTransientFields(sales);
        return sales;
    }

    // Nuevo método para obtener ventas por estado paginador
    public Page<Sale> getSalesPageableByActiveStatus(String active, Pageable pageable) {
        List<Sale> allActiveSales = saleRepository.findByActive(active);
        allActiveSales.sort(Comparator.comparing(Sale::getId).reversed());

        List<Sale> formattedSales = allActiveSales.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        setTransientFields(formattedSales);
        return new PageImpl<>(formattedSales, pageable, allActiveSales.size());
    }

    // Método para obtener una venta por su ID
    public Optional<Sale> getSaleById(Long id) {
        Optional<Sale> sale = saleRepository.findById(id);
        sale.ifPresent(this::setTransientFields);
        return sale;
    }

    // Método para crear una venta
    public Sale createSale(Sale sale) {
        sale.setActive("A");
        // Calcular los subtotales y el total de la venta
        calculateSaleTotals(sale);
        // Establecer la relación bidireccional
        if (sale.getSaleDetails() != null) {
            for (SaleDetail detail : sale.getSaleDetails()) {
                detail.setSale(sale);
            }
        }
        Sale savedSale = saleRepository.save(sale);
        setTransientFields(savedSale);
        return savedSale;
    }

    // Método para eliminar una venta
    public void deleteSale(Long id) {
        saleRepository.deleteById(id);
    }

    // Método para eliminar una venta de manera lógica (cambia el estado a 'I')
    public Sale logicalDeleteSale(Long id) {
        return saleRepository.findById(id)
                .map(sale -> {
                    if ("I".equals(sale.getActive())) {
                        throw new ResourceConflictException("Sale with id " + id + " is already inactive");
                    }
                    sale.setActive("I");
                    return saleRepository.save(sale);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id " + id));
    }

    // Método para activar una venta de manera lógica (cambia el estado a 'A')
    public Sale logicalActivateSale(Long id) {
        return saleRepository.findById(id)
                .map(sale -> {
                    if ("A".equals(sale.getActive())) {
                        throw new ResourceConflictException("Sale with id " + id + " is already active");
                    }
                    sale.setActive("A");
                    return saleRepository.save(sale);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id " + id));
    }

    // Método para actualizar una venta
    public Sale updateSale(Long id, Sale saleUpdated) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id " + id));
        // Actualizar datos de la venta
        sale.setClient(saleUpdated.getClient());
        sale.setSeller(saleUpdated.getSeller());
        sale.setPaymentMethod(saleUpdated.getPaymentMethod());
        // Establecer el campo dateTime con la fecha actual del servidor si no se proporciona ningún valor
        if (saleUpdated.getDateTime() == null) {
            sale.setDateTime(new Date()); // Fecha actual del servidor
        } else {
            sale.setDateTime(saleUpdated.getDateTime());
        }
        // Manejar los detalles de la venta (SaleDetails)
        updateSaleDetails(sale, saleUpdated.getSaleDetails());
        // Calcular los subtotales y el total de la venta
        calculateSaleTotals(sale);
        // Guardar la venta actualizada
        Sale savedSale = saleRepository.save(sale);
        setTransientFields(savedSale);
        return savedSale;
    }

    // Método para actualizar los detalles de la venta
    private void updateSaleDetails(Sale sale, List<SaleDetail> updatedDetails) {
        // Mapa de detalles actuales para una búsqueda rápida por ID
        Map<Long, SaleDetail> currentDetailsMap = sale.getSaleDetails().stream()
                .collect(Collectors.toMap(SaleDetail::getId, detail -> detail));
        // Procesar los detalles actualizados
        for (SaleDetail detail : updatedDetails) {
            if (detail.getId() == null) {
                // Nuevo detalle, agregar a la venta
                detail.setSale(sale);
                sale.getSaleDetails().add(detail);
            } else if (currentDetailsMap.containsKey(detail.getId())) {
                // Detalle existente, actualizar
                SaleDetail existingDetail = currentDetailsMap.get(detail.getId());
                existingDetail.setProduct(detail.getProduct());
                existingDetail.setAmount(detail.getAmount());
                // No es necesario eliminarlo del mapa porque no es un detalle que vamos a eliminar
            }
        }
        // Eliminar los detalles que no están en los detalles actualizados
        sale.getSaleDetails().removeIf(detail -> !updatedDetails.stream()
                .map(SaleDetail::getId)
                .collect(Collectors.toList())
                .contains(detail.getId()));
    }

    // Método para calcular los subtotales y el total de la venta
    private void calculateSaleTotals(Sale sale) {
        double total = 0;
        for (SaleDetail detail : sale.getSaleDetails()) {
            Product product = productRepo.findById(detail.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + detail.getProduct().getId()));
            double subtotal = product.getPriceUnit() * detail.getAmount();
            detail.setSubtotalSale(subtotal);
            total += subtotal;
        }
        sale.setTotalSale(total);
    }

    // Método para establecer los campos transitorios
    private void setTransientFields(List<Sale> sales) {
        for (Sale sale : sales) {
            setTransientFields(sale);
        }
    }

    private void setTransientFields(Sale sale) {
        if (sale.getClient() != null) {
            sale.setClientNames(sale.getClient().getNames() + " " + sale.getClient().getLastName());
        }
        if (sale.getSeller() != null) {
            sale.setSellerNames(sale.getSeller().getNames() + " " + sale.getSeller().getLastName());
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