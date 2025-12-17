package pe.edu.vallegrande.sistventas.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sistventas.model.PaymentMethod;
import pe.edu.vallegrande.sistventas.service.PaymentMethodService;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/v1/paymentMethod")
public class PaymentMethodRest {
    private final PaymentMethodService paymentMethodService;

    @Autowired
    public PaymentMethodRest(PaymentMethodService paymentMethodService){
        this.paymentMethodService = paymentMethodService;
    }

    // Endpoint para listar todas las categorias
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getAllPayment() {
        return paymentMethodService.getPaymentMethod();
    }

    // Endpoint para listar categorias activos
    @GetMapping("/active")
    public ResponseEntity<List<PaymentMethod>> getActivePayment() {
        return paymentMethodService.getActivePayment();
    }

    // Endpoint para listar categorias inactivos
    @GetMapping("/inactive")
    public ResponseEntity<List<PaymentMethod>> getInactivePayment() {
        return paymentMethodService.getInactivePayment();
    }

    // Endpoint para obtener un cliente por su ID
    @GetMapping("/{paymentId}")
    public ResponseEntity<Object> getPaymentById(@PathVariable("paymentId") Long id) {
        return paymentMethodService.getPaymentById(id);
    }
}
