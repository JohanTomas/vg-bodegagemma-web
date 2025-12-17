package pe.edu.vallegrande.sistventas.model.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryReportData {
    private BigDecimal ID;
    private String NAME;
    private String DESCRIPTION;
    private String ACTIVE;
}
