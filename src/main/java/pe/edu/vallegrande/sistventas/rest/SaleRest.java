package pe.edu.vallegrande.sistventas.rest;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxExporterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sistventas.dto.Sale;
import pe.edu.vallegrande.sistventas.dto.SaleDetail;
import pe.edu.vallegrande.sistventas.dto.reports.SaleReportData;
import pe.edu.vallegrande.sistventas.service.SaleService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/sales")
public class SaleRest {
    private static final Logger logger = LoggerFactory.getLogger(SaleRest.class);

    @Autowired
    private SaleService saleService;

    // Endpoint para obtener todas las ventas
    @GetMapping
    public List<Sale> getAllSales() {
        return saleService.getAllSales();
    }

    // Endpoint para obtener una venta por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        Optional<Sale> sale = saleService.getSaleById(id);
        return sale.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint para crear una nueva venta
    @PostMapping
    public Sale createSale(@RequestBody Sale sale) {
        return saleService.createSale(sale);
    }

    // Endpoint para actualizar una venta
    @PutMapping("/{id}")
    public ResponseEntity<Sale> updateSale(@PathVariable Long id, @RequestBody Sale sale) {
        try {
            Sale updatedSale = saleService.updateSale(id, sale);
            return ResponseEntity.ok(updatedSale);
        } catch (SaleService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating sale: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para eliminar una venta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        Optional<Sale> sale = saleService.getSaleById(id);
        if (sale.isPresent()) {
            saleService.deleteSale(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Nuevo endpoint para obtener ventas por estado
    @GetMapping("/status/{active}")
    public List<Sale> getSalesByActiveStatus(@PathVariable String active) {
        return saleService.getSalesByActiveStatus(active);
    }

    // Nuevo endpoint para obtener ventas por estado con paginación
    @GetMapping("/status/{active}/page")
    public Page<Sale> getSalesPageableByActiveStatus(@PathVariable String active, Pageable pageable) {
        return saleService.getSalesPageableByActiveStatus(active, pageable);
    }

    // Endpoint para eliminar lógicamente una venta (cambia el estado a 'I')
    @PutMapping("/delete/{id}")
    public ResponseEntity<Sale> logicalDeleteSale(@PathVariable Long id) {
        try {
            Sale sale = saleService.logicalDeleteSale(id);
            return ResponseEntity.ok(sale);
        } catch (SaleService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (SaleService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para activar lógicamente una venta (cambia el estado 'A')
    @PutMapping("/activate/{id}")
    public ResponseEntity<Sale> logicalActivateSale(@PathVariable Long id) {
        try {
            Sale sale = saleService.logicalActivateSale(id);
            return ResponseEntity.ok(sale);
        } catch (SaleService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (SaleService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    //REPORTES
    // Endpoint para generar reporte PDF
    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> generateSaleReport(@PathVariable Long id) {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Cargar el archivo de diseño del reporte de venta unitaria
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_ventaUnitaria.jasper");

            // Obtener la venta por ID
            Optional<Sale> optionalSale = this.saleService.getSaleById(id);

            if (optionalSale.isEmpty()) {
                // Si no se encuentra la venta, devolver error 404
                return ResponseEntity.notFound().build();
            }

            Sale sale = optionalSale.get();

            // Convertir los objetos Sale a SaleReportData
            List<SaleReportData> saleReportData = new ArrayList<>();
            for (SaleDetail detail : sale.getSaleDetails()) {
                SaleReportData data = new SaleReportData();
                data.setSALE_ID(BigDecimal.valueOf(sale.getId()));
                String formattedDate = sale.getFormattedDateTime();
                data.setSALE_DATE(formattedDate);
                data.setCLIENT_NAME(sale.getClient().getNames() + " " + sale.getClient().getLastName());
                data.setSELLER_NAME(sale.getSeller().getNames() + " " + sale.getSeller().getLastName());
                data.setPRODUCT_NAME(detail.getProduct().getName());
                data.setUNIT_SALE(detail.getProduct().getUnitSale());
                data.setPAYMENT_METHOD(detail.getSale().getPaymentMethod().getName());
                data.setQUANTITY(BigDecimal.valueOf(detail.getAmount()));
                data.setPRICE_UNIT(BigDecimal.valueOf(detail.getProduct().getPriceUnit()));
                data.setSUBTOTAL_PRODUCT(BigDecimal.valueOf(detail.getSubtotalSale()));
                data.setTOTAL(BigDecimal.valueOf(detail.getSale().getTotalSale()));

                saleReportData.add(data);
            }

            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(saleReportData);

            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);

            // Exportar el reporte a un array de bytes en formato PDF
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);

            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("sale_report.pdf").build());

            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            // Manejar las excepciones y devolver el estado adecuado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }

    // Endpoint para generar reporte en Excel
    @GetMapping("/report/excel/{id}")
    public ResponseEntity<byte[]> generateExcelSaleReport(@PathVariable Long id) {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Cargar el archivo de diseño del reporte de venta unitaria
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_ventaUnitaria.jasper");

            // Obtener la venta por ID
            Optional<Sale> optionalSale = this.saleService.getSaleById(id);

            if (optionalSale.isEmpty()) {
                // Si no se encuentra la venta, devolver error 404
                return ResponseEntity.notFound().build();
            }

            Sale sale = optionalSale.get();

            // Convertir los objetos Sale a SaleReportData
            List<SaleReportData> saleReportData = new ArrayList<>();
            for (SaleDetail detail : sale.getSaleDetails()) {
                SaleReportData data = new SaleReportData();
                data.setSALE_ID(BigDecimal.valueOf(sale.getId()));
                String formattedDate = sale.getFormattedDateTime();
                data.setSALE_DATE(formattedDate);
                data.setCLIENT_NAME(sale.getClient().getNames() + " " + sale.getClient().getLastName());
                data.setSELLER_NAME(sale.getSeller().getNames() + " " + sale.getSeller().getLastName());
                data.setPRODUCT_NAME(detail.getProduct().getName());
                data.setUNIT_SALE(detail.getProduct().getUnitSale());
                data.setPAYMENT_METHOD(detail.getSale().getPaymentMethod().getName());
                data.setQUANTITY(BigDecimal.valueOf(detail.getAmount()));
                data.setPRICE_UNIT(BigDecimal.valueOf(detail.getProduct().getPriceUnit()));
                data.setSUBTOTAL_PRODUCT(BigDecimal.valueOf(detail.getSubtotalSale()));
                data.setTOTAL(BigDecimal.valueOf(detail.getSale().getTotalSale()));

                saleReportData.add(data);
            }

            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(saleReportData);

            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);

            // Exportar el reporte a un array de bytes en formato XLSX
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
            SimpleXlsxExporterConfiguration configuration = new SimpleXlsxExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("sale_report.xlsx").build());

            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            // Manejar las excepciones y devolver el estado adecuado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
}
