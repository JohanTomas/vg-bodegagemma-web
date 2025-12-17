package pe.edu.vallegrande.sistventas.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleReportData {
    private BigDecimal SALE_ID;
    private String SALE_DATE;
    private String PAYMENT_METHOD;
    private String CLIENT_NAME;
    private String SELLER_NAME;
    private String PRODUCT_NAME;
    private String UNIT_SALE;
    private BigDecimal QUANTITY;
    private BigDecimal PRICE_UNIT;
    private BigDecimal SUBTOTAL_PRODUCT;
    private BigDecimal TOTAL;
}
