package pe.edu.vallegrande.sistventas.model.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellerReportData {
    private BigDecimal ID;
    private String TYPE_DOCUMENT;
    private String NUMBER_DOCUMENT;
    private String NAMES;
    private String LAST_NAME;
    private String CELL_PHONE;
    private String EMAIL;
    private BigDecimal SALARY;
    private String SELLER_USER;
    private String SELLER_PASSWORD;

}
