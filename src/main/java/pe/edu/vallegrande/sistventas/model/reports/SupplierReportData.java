package pe.edu.vallegrande.sistventas.model.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierReportData {
    private BigDecimal ID;
    private String RUC;
    private String NAME_COMPANY;
    private String NAMES;
    private String LAST_NAME;
    private String CELL_PHONE;
    private String EMAIL;
    private String ACTIVE;
}
