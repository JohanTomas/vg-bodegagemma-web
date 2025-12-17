package pe.edu.vallegrande.sistventas.model.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientReportData {
    private BigDecimal ID;
    private String TYPE_DOCUMENT;
    private String NUMBER_DOCUMENT;
    private String NAMES;
    private String LAST_NAME;
    private String CELL_PHONE;
    private String EMAIL;
    private String BIRTHDATE;
}
