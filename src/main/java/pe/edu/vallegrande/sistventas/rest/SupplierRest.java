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
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sistventas.model.Supplier;
import pe.edu.vallegrande.sistventas.model.reports.SupplierReportData;
import pe.edu.vallegrande.sistventas.service.SupplierService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/v1/suppliers")
public class SupplierRest {
    private final SupplierService supplierService;
    private static final Logger logger = LoggerFactory.getLogger(SupplierRest.class);

    @Autowired
    public SupplierRest(SupplierService supplierService){
        this.supplierService = supplierService;
    }

    // Endpoint para listar todas los proveedores
    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return this.supplierService.getSuppliers();
    }

    // Endpoint para listar proveedores activos
    @GetMapping("/active")
    public List<Supplier> getActiveClients() {
        return this.supplierService.getActiveSuppliers();
    }

    // Endpoint para listar proveedores inactivos
    @GetMapping("/inactive")
    public List<Supplier> getInactiveClients() {
        return this.supplierService.getInactiveSuppliers();
    }
    // Endpoint para buscar una persona por numero Documento
    @GetMapping("/exists")
    public ResponseEntity<Object> checkIfSellerExists(@RequestParam String numberDocument) {
        List<Supplier> supplier = supplierService.getExistingSuppliers(numberDocument);
        if (!supplier.isEmpty()) {
            return ResponseEntity.ok(supplier.get(0)); // Devuelve el primer cliente encontrado
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // Endpoint para obtener un proveedores por su ID
    @GetMapping("/{supplierId}")
    public ResponseEntity<Object> getSupplierById(@PathVariable("supplierId") Long id) {
        return supplierService.getSupplierById(id);
    }

    //LISTAR PAGINADOR A ACTIVOS CLIENTES
    @GetMapping("/active/pageable")
    public Page<Supplier> listPageableSuppliersA(Pageable pageable) {
        return this.supplierService.listPageableSuppliersA(pageable);
    }

    //LISTAR PAGINADOR A INACTIVOS CLIENTES
    @GetMapping("/inactive/pageable")
    public Page<Supplier> listPageableSuppliersI(Pageable pageable) {
        return this.supplierService.listPageableSuppliersI(pageable);
    }

    // Ingresar proveedor
    @PostMapping
    public ResponseEntity<Object> registrarSupplier(@RequestBody Supplier supplier){
        return this.supplierService.newSupplier(supplier);
    }

    // Actualizar proveedor
    @PutMapping("/{supplierId}")
    public ResponseEntity<Object> actualizarSupplier(@PathVariable("supplierId") Long id, @RequestBody Supplier supplier){
        return this.supplierService.updateSupplier(id, supplier);
    }

    // Eliminado fisicamente
    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Object> eliminarSupplier(@PathVariable("supplierId") Long id){
        return this.supplierService.deletesupplier(id);
    }

    // Eliminado Logico
    @PutMapping("/disable/{supplierId}")
    public ResponseEntity<Object> deleteSupplierById(@PathVariable("supplierId") Long id) {
        return this.supplierService.deleteSupplierById(id);
    }

    // Activar Cliente
    @PutMapping("/activate/{supplierId}")
    public ResponseEntity<Object> activateSupplierById(@PathVariable("supplierId") Long id) {
        return this.supplierService.activateSupplierById(id);
    }

    //REPORTES
    @GetMapping("/report")
    public ResponseEntity<byte[]> generateReport() {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_supplier.jasper");
            // Obtener los datos para el reporte
            List<Supplier> suppliers = this.supplierService.getActiveSuppliers();
            // Convertir los objetos Person a ClientReportData
            List<SupplierReportData> supplierReportData = suppliers.stream()
                    .map(supplier -> {
                        SupplierReportData data = new SupplierReportData();
                        data.setID(BigDecimal.valueOf(supplier.getId()));
                        data.setRUC(supplier.getRuc());
                        data.setNAME_COMPANY(supplier.getNameCompany());
                        data.setNAMES(supplier.getNames());
                        data.setLAST_NAME(supplier.getLastName());
                        data.setCELL_PHONE(supplier.getCellPhone());
                        data.setEMAIL(supplier.getEmail());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(supplierReportData);
            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);
            // Exportar el reporte a un array de bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("client_report.pdf").build());
            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (JRException e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (ParserConfigurationException e) {
            logger.error("Error configuring document builder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    @GetMapping("/report/excel")
    public ResponseEntity<byte[]> generateExcelReport() {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_supplier.jasper");
            // Obtener los datos para el reporte
            List<Supplier> suppliers = this.supplierService.getActiveSuppliers();
            // Convertir los objetos Person a ClientReportData
            List<SupplierReportData> supplierReportData = suppliers.stream()
                    .map(supplier -> {
                        SupplierReportData data = new SupplierReportData();
                        data.setID(BigDecimal.valueOf(supplier.getId()));
                        data.setRUC(supplier.getRuc());
                        data.setNAME_COMPANY(supplier.getNameCompany());
                        data.setNAMES(supplier.getNames());
                        data.setLAST_NAME(supplier.getLastName());
                        data.setCELL_PHONE(supplier.getCellPhone());
                        data.setEMAIL(supplier.getEmail());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(supplierReportData);
            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);
            // Exportar el reporte a un array de bytes en formato XLSX
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
            SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(byteArrayOutputStream);
            SimpleXlsxExporterConfiguration configuration = new SimpleXlsxExporterConfiguration();
            exporter.setExporterInput(exporterInput);
            exporter.setExporterOutput(exporterOutput);
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("client_report.xlsx").build());
            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (JRException e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (ParserConfigurationException e) {
            logger.error("Error configuring document builder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    //REPORTES inactivos
    @GetMapping("/inactive/report")
    public ResponseEntity<byte[]> generateReportInactive() {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_supplier.jasper");
            // Obtener los datos para el reporte
            List<Supplier> suppliers = this.supplierService.getInactiveSuppliers();
            // Convertir los objetos Person a ClientReportData
            List<SupplierReportData> supplierReportData = suppliers.stream()
                    .map(supplier -> {
                        SupplierReportData data = new SupplierReportData();
                        data.setID(BigDecimal.valueOf(supplier.getId()));
                        data.setRUC(supplier.getRuc());
                        data.setNAME_COMPANY(supplier.getNameCompany());
                        data.setNAMES(supplier.getNames());
                        data.setLAST_NAME(supplier.getLastName());
                        data.setCELL_PHONE(supplier.getCellPhone());
                        data.setEMAIL(supplier.getEmail());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(supplierReportData);
            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);
            // Exportar el reporte a un array de bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("client_report.pdf").build());
            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (JRException e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (ParserConfigurationException e) {
            logger.error("Error configuring document builder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    @GetMapping("inactive/report/excel")
    public ResponseEntity<byte[]> generateExcelReportInactive() {
        try {
            // Configurar el DocumentBuilderFactory manualmente
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Cargar el archivo de diseño del reporte
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_supplier.jasper");
            // Obtener los datos para el reporte
            List<Supplier> suppliers = this.supplierService.getInactiveSuppliers();
            // Convertir los objetos Person a ClientReportData
            List<SupplierReportData> supplierReportData = suppliers.stream()
                    .map(supplier -> {
                        SupplierReportData data = new SupplierReportData();
                        data.setID(BigDecimal.valueOf(supplier.getId()));
                        data.setRUC(supplier.getRuc());
                        data.setNAME_COMPANY(supplier.getNameCompany());
                        data.setNAMES(supplier.getNames());
                        data.setLAST_NAME(supplier.getLastName());
                        data.setCELL_PHONE(supplier.getCellPhone());
                        data.setEMAIL(supplier.getEmail());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(supplierReportData);
            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);
            // Exportar el reporte a un array de bytes en formato XLSX
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            SimpleExporterInput exporterInput = new SimpleExporterInput(jasperPrint);
            SimpleOutputStreamExporterOutput exporterOutput = new SimpleOutputStreamExporterOutput(byteArrayOutputStream);
            SimpleXlsxExporterConfiguration configuration = new SimpleXlsxExporterConfiguration();
            exporter.setExporterInput(exporterInput);
            exporter.setExporterOutput(exporterOutput);
            exporter.setConfiguration(configuration);
            exporter.exportReport();
            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("client_report.xlsx").build());
            // Devolver el reporte como respuesta
            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (JRException e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (ParserConfigurationException e) {
            logger.error("Error configuring document builder: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }
}
