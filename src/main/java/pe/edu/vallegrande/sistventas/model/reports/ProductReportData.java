package pe.edu.vallegrande.sistventas.model.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReportData {
    private BigDecimal PRODUCT_ID;
    private String PRODUCT_CODE;
    private String PRODUCT_NAME;
    private String CATEGORY_NAME;
    private BigDecimal PRICE_UNIT;
    private String UNIT_SALE;
    private String DATE_EXPIRY;
    private BigDecimal STOCK;
    private String ACTIVE;
}
