package pe.edu.vallegrande.sistventas.rest;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
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
import pe.edu.vallegrande.sistventas.dto.Purchase;
import pe.edu.vallegrande.sistventas.dto.PurchaseDetail;
import pe.edu.vallegrande.sistventas.dto.reports.PurchaseReportData;
import pe.edu.vallegrande.sistventas.service.PurchaseService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/purchases")
public class PurchaseRest {
    private static final Logger logger = LoggerFactory.getLogger(ProductRest.class);
    @Autowired
    private PurchaseService purchaseService;

    // Endpoint para obtener todas las compras
    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseService.getAllPurchases();
    }

    // Endpoint para obtener una compra por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Purchase> getPurchaseById(@PathVariable Long id) {
        Optional<Purchase> purchase = purchaseService.getPurchaseById(id);
        return purchase.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint para crear una nueva compra
    @PostMapping
    public Purchase createPurchase(@RequestBody Purchase purchase) {
        return purchaseService.createPurchase(purchase);
    }

    // Endpoint para actualizar una compra
    @PutMapping("/{id}")
    public ResponseEntity<Purchase> updatePurchase(@PathVariable Long id, @RequestBody Purchase purchase) {
        try {
            Purchase updatedPurchase = purchaseService.updatePurchase(id, purchase);
            return ResponseEntity.ok(updatedPurchase);
        } catch (PurchaseService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating purchase: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para eliminar una compra
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id) {
        Optional<Purchase> purchase = purchaseService.getPurchaseById(id);
        if (purchase.isPresent()) {
            purchaseService.deletePurchase(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Nuevo endpoint para obtener compras por estado
    @GetMapping("/status/{active}")
    public List<Purchase> getPurchasesByActiveStatus(@PathVariable String active) {
        return purchaseService.getPurchasesByActiveStatus(active);
    }

    // Nuevo endpoint para obtener compras por estado con paginación
    @GetMapping("/status/{active}/page")
    public Page<Purchase> getPurchasesPageableByActiveStatus(@PathVariable String active, Pageable pageable) {
        return purchaseService.getPurchasesPageableByActiveStatus(active, pageable);
    }

    // Endpoint para eliminar lógicamente una compra (cambia el estado a 'I')
    @PutMapping("/delete/{id}")
    public ResponseEntity<Purchase> logicalDeletePurchase(@PathVariable Long id) {
        try {
            Purchase purchase = purchaseService.logicalDeletPurchase(id);
            return ResponseEntity.ok(purchase);
        } catch (PurchaseService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (PurchaseService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para activar lógicamente una compra (cambia el estado 'A')
    @PutMapping("/activate/{id}")
    public ResponseEntity<Purchase> logicalActivatePurchase(@PathVariable Long id) {
        try {
            Purchase purchase = purchaseService.logicalActivarPurchase(id);
            return ResponseEntity.ok(purchase);
        } catch (PurchaseService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (PurchaseService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    //REPORTES
    // Endpoint para generar reporte PDF
    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> generateReport(@PathVariable Long id) {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_compraUnitaria.jasper");

            // Obtener la compra por ID
            Optional<Purchase> optionalPurchase = this.purchaseService.getPurchaseById(id);

            if (optionalPurchase.isEmpty()) {
                // Si no se encuentra la compra, devolver error 404
                return ResponseEntity.notFound().build();
            }

            Purchase purchase = optionalPurchase.get();

            // Convertir los objetos Purchase a PurchaseReportData
            List<PurchaseReportData> purchaseReportData = new ArrayList<>();
            for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
                PurchaseReportData data = new PurchaseReportData();
                data.setID_COMPRA(BigDecimal.valueOf(purchase.getId()));
                data.setEMPRESA(detail.getPurchase().getSupplier().getNameCompany());
                String formattedDate = purchase.getFormattedDateTime();
                data.setFECHA_COMPRA(formattedDate);
                data.setPROVEEDOR(purchase.getSupplier().getNames() + " " + purchase.getSupplier().getLastName());
                data.setVENDEDOR(purchase.getSeller().getNames() + " " + purchase.getSeller().getLastName());
                data.setPRODUCTO(detail.getProduct().getName());
                data.setTIPO_PAGO(detail.getPurchase().getPaymentMethod().getName());
                data.setCANTIDAD(BigDecimal.valueOf(detail.getAmount()));
                data.setPRECIO_UNITARIO(BigDecimal.valueOf(detail.getPriceUnit()));
                // Calcular el subtotal como la multiplicación de la cantidad por el precio unitario
                BigDecimal subtotal = BigDecimal.valueOf(detail.getAmount())
                        .multiply(BigDecimal.valueOf(detail.getPriceUnit()));
                data.setSUBTOTAL(subtotal);
                data.setTOTAL(BigDecimal.valueOf(detail.getPurchase().getTotalPurchase()));
                purchaseReportData.add(data);
            }

            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(purchaseReportData);

            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);

            // Exportar el reporte a un array de bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);

            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("purchase_report.pdf").build());

            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            // Manejar las excepciones y devolver el estado adecuado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }

    // Endpoint para generar reporte en Excel
    @GetMapping("/report/excel/{id}")
    public ResponseEntity<byte[]> generateExcelReport(@PathVariable Long id) {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_compraUnitaria.jasper");

            // Obtener la compra por ID
            Optional<Purchase> optionalPurchase = this.purchaseService.getPurchaseById(id);

            if (optionalPurchase.isEmpty()) {
                // Si no se encuentra la compra, devolver error 404
                return ResponseEntity.notFound().build();
            }

            Purchase purchase = optionalPurchase.get();

            // Convertir los objetos Purchase a PurchaseReportData
            List<PurchaseReportData> purchaseReportData = new ArrayList<>();
            for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
                PurchaseReportData data = new PurchaseReportData();
                data.setID_COMPRA(BigDecimal.valueOf(purchase.getId()));
                data.setEMPRESA(detail.getPurchase().getSupplier().getNameCompany());
                String formattedDate = purchase.getFormattedDateTime();
                data.setFECHA_COMPRA(formattedDate);
                data.setPROVEEDOR(purchase.getSupplier().getNames() + " " + purchase.getSupplier().getLastName());
                data.setVENDEDOR(purchase.getSeller().getNames() + " " + purchase.getSeller().getLastName());
                data.setPRODUCTO(detail.getProduct().getName());
                data.setTIPO_PAGO(detail.getPurchase().getPaymentMethod().getName());
                data.setCANTIDAD(BigDecimal.valueOf(detail.getAmount()));
                data.setPRECIO_UNITARIO(BigDecimal.valueOf(detail.getPriceUnit()));
                // Calcular el subtotal como la multiplicación de la cantidad por el precio unitario
                BigDecimal subtotal = BigDecimal.valueOf(detail.getAmount())
                        .multiply(BigDecimal.valueOf(detail.getPriceUnit()));
                data.setSUBTOTAL(subtotal);
                data.setTOTAL(BigDecimal.valueOf(detail.getPurchase().getTotalPurchase()));
                purchaseReportData.add(data);
            }

            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(purchaseReportData);

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
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("purchase_report.xlsx").build());

            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            // Manejar las excepciones y devolver el estado adecuado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
}
