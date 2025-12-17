package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sistventas.model.PaymentMethod;
import pe.edu.vallegrande.sistventas.repository.PaymentMethodRepo;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepo paymentMethodRepo;

    @Autowired
    public PaymentMethodService(PaymentMethodRepo paymentMethodRepo) {
        this.paymentMethodRepo = paymentMethodRepo;
    }

    // Listado completo de todas las categorías en orden descendente
    public ResponseEntity<List<PaymentMethod>> getPaymentMethod() {
        List<PaymentMethod> paymentMethods = this.paymentMethodRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(paymentMethods);
    }

    // Listar métodos de pago activos
    public ResponseEntity<List<PaymentMethod>> getActivePayment() {
        List<PaymentMethod> activePayments = this.paymentMethodRepo.findByActive("A").stream()
                .sorted(Comparator.comparing(PaymentMethod::getId).reversed()) // Ordenar en orden descendente
                .collect(Collectors.toList());
        return ResponseEntity.ok(activePayments);
    }

    // Listar métodos de pago inactivos
    public ResponseEntity<List<PaymentMethod>> getInactivePayment() {
        List<PaymentMethod> inactivePayments = this.paymentMethodRepo.findByActive("I").stream()
                .sorted(Comparator.comparing(PaymentMethod::getId).reversed()) // Ordenar en orden descendente
                .collect(Collectors.toList());
        return ResponseEntity.ok(inactivePayments);
    }

    // Obtener método de pago por ID
    public ResponseEntity<Object> getPaymentById(Long id) {
        Optional<PaymentMethod> optionalPaymentMethod = paymentMethodRepo.findById(id);
        HashMap<String, Object> responseData = new HashMap<>();
        if (optionalPaymentMethod.isPresent()) {
            PaymentMethod paymentMethod = optionalPaymentMethod.get();
            responseData.put("data", paymentMethod);
            return new ResponseEntity<>(responseData, HttpStatus.OK);
        } else {
            responseData.put("error", true);
            responseData.put("mensaje", "No se encontró un método de pago con el ID proporcionado.");
            return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
        }
    }
}
