package pe.edu.vallegrande.sistventas.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReportData {
    private BigDecimal ID_COMPRA;
    private String FECHA_COMPRA;
    private String EMPRESA;
    private String PROVEEDOR;
    private String VENDEDOR;
    private String PRODUCTO;
    private String TIPO_PAGO;
    private BigDecimal CANTIDAD;
    private BigDecimal PRECIO_UNITARIO;
    private BigDecimal SUBTOTAL;
    private BigDecimal TOTAL;
}
