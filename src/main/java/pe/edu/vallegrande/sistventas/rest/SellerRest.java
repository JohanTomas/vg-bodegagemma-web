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
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.model.reports.SellerReportData;
import pe.edu.vallegrande.sistventas.service.SellerService;

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
@RequestMapping("api/v1/sellers")
public class SellerRest {
    private final SellerService sellerService;
    private static final Logger logger = LoggerFactory.getLogger(SellerRest.class);

    @Autowired
    public SellerRest(SellerService sellerService){
        this.sellerService = sellerService;
    }
    // Endpoint para listar todas las personas
    @GetMapping
    public List<Person> getAllPersons() {
        return this.sellerService.getPersons();
    }
    // Endpoint para buscar una persona por número de documento y rol "V"
    @GetMapping("/exists")
    public ResponseEntity<Object> checkIfSellerExists(@RequestParam String numberDocument) {
        List<Person> sellers = sellerService.getExistingSellers(numberDocument);
        if (!sellers.isEmpty()) {
            return ResponseEntity.ok(sellers.get(0)); // Devuelve el primer cliente encontrado
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // Endpoint para listar todos los vendedores
    @GetMapping("/list")
    public List<Person> getAllSellers() {
        return this.sellerService.getSellers();
    }
    // Endpoint para listar clientes activos
    @GetMapping("/active")
    public List<Person> getActiveClients() {
        return this.sellerService.getActiveSellers();
    }
    // Endpoint para listar clientes inactivos
    @GetMapping("/inactive")
    public List<Person> getInactiveClients() {
        return this.sellerService.getInactiveSellers();
    }
    // Endpoint para obtener un cliente por su ID
    @GetMapping("/{sellerId}")
    public ResponseEntity<Object> getSellerById(@PathVariable("sellerId") Long id) {
        return sellerService.getSellerById(id);
    }
    //LISTAR PAGINADOR A ACTIVOS CLIENTES
    @GetMapping("/active/pageable")
    public Page<Person> listPageableClientsA(Pageable pageable) {
        return this.sellerService.listPageableSellerA(pageable);
    }
    //LISTAR PAGINADOR A INACTIVOS CLIENTES
    @GetMapping("/inactive/pageable")
    public Page<Person> listPageableSellerI(Pageable pageable) {
        return this.sellerService.listPageableSellerI(pageable);
    }
    // Ingresar Vendedor
    @PostMapping
    public ResponseEntity<Object> registrarSeller(@RequestBody Person seller){
        return this.sellerService.newSeller(seller);
    }
    // Actualizar Vendedor
    @PutMapping("/{sellerId}")
    public ResponseEntity<Object> actualizarSeller(@PathVariable("sellerId") Long id, @RequestBody Person seller){
        return this.sellerService.updateSeller(id, seller);
    }

    // Eliminado fisicamente
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Object> eliminarClient(@PathVariable("clientId") Long id){
        return this.sellerService.deleteseller(id);
    }

    // Eliminado Logico
    @PutMapping("/disable/{clientId}")
    public ResponseEntity<Object> disableSellerById(@PathVariable("clientId") Long id) {
        return this.sellerService.deleteSellerById(id);
    }

    // Activar Cliente
    @PutMapping("/activate/{clientId}")
    public ResponseEntity<Object> activateSellerById(@PathVariable("clientId") Long id) {
        return this.sellerService.activateSellerById(id);
    }
    // Endpoint para cambiar la contraseña del vendedor
    @PutMapping("/{sellerId}/change-password")
    public ResponseEntity<Object> changeSellerPassword(@PathVariable("sellerId") Long id, @RequestBody Map<String, String> passwordData) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        return this.sellerService.changePassword(id, oldPassword, newPassword);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_seller.jasper");
            // Obtener los datos para el reporte
            List<Person> sellers = this.sellerService.getActiveSellers();
            // Convertir los objetos Person a ClientReportData
            List<SellerReportData> sellerReportData = sellers.stream()
                    .map(seller -> {
                        SellerReportData data = new SellerReportData();
                        data.setID(BigDecimal.valueOf(seller.getId()));
                        data.setTYPE_DOCUMENT(seller.getTypeDocument());
                        data.setNUMBER_DOCUMENT(seller.getNumberDocument());
                        data.setNAMES(seller.getNames());
                        data.setLAST_NAME(seller.getLastName());
                        data.setCELL_PHONE(seller.getCellPhone());
                        data.setEMAIL(seller.getEmail());
                        data.setSALARY(BigDecimal.valueOf(seller.getSalary()));
                        data.setSELLER_USER(seller.getSellerUser());
                        data.setSELLER_PASSWORD(seller.getSellerPassword());

                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(sellerReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_seller.jasper");
            // Obtener los datos para el reporte
            List<Person> sellers = this.sellerService.getActiveSellers();
            // Convertir los objetos Person a ClientReportData
            List<SellerReportData> sellerReportData = sellers.stream()
                    .map(seller -> {
                        SellerReportData data = new SellerReportData();
                        data.setID(BigDecimal.valueOf(seller.getId()));
                        data.setTYPE_DOCUMENT(seller.getTypeDocument());
                        data.setNUMBER_DOCUMENT(seller.getNumberDocument());
                        data.setNAMES(seller.getNames());
                        data.setLAST_NAME(seller.getLastName());
                        data.setCELL_PHONE(seller.getCellPhone());
                        data.setEMAIL(seller.getEmail());
                        data.setSALARY(BigDecimal.valueOf(seller.getSalary()));
                        data.setSELLER_USER(seller.getSellerUser());
                        data.setSELLER_PASSWORD(seller.getSellerPassword());

                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(sellerReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_seller.jasper");
            // Obtener los datos para el reporte
            List<Person> sellers = this.sellerService.getInactiveSellers();
            // Convertir los objetos Person a ClientReportData
            List<SellerReportData> sellerReportData = sellers.stream()
                    .map(seller -> {
                        SellerReportData data = new SellerReportData();
                        data.setID(BigDecimal.valueOf(seller.getId()));
                        data.setTYPE_DOCUMENT(seller.getTypeDocument());
                        data.setNUMBER_DOCUMENT(seller.getNumberDocument());
                        data.setNAMES(seller.getNames());
                        data.setLAST_NAME(seller.getLastName());
                        data.setCELL_PHONE(seller.getCellPhone());
                        data.setEMAIL(seller.getEmail());
                        data.setSALARY(BigDecimal.valueOf(seller.getSalary()));
                        data.setSELLER_USER(seller.getSellerUser());
                        data.setSELLER_PASSWORD(seller.getSellerPassword());

                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(sellerReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_seller.jasper");
            // Obtener los datos para el reporte
            List<Person> sellers = this.sellerService.getInactiveSellers();
            // Convertir los objetos Person a ClientReportData
            List<SellerReportData> sellerReportData = sellers.stream()
                    .map(seller -> {
                        SellerReportData data = new SellerReportData();
                        data.setID(BigDecimal.valueOf(seller.getId()));
                        data.setTYPE_DOCUMENT(seller.getTypeDocument());
                        data.setNUMBER_DOCUMENT(seller.getNumberDocument());
                        data.setNAMES(seller.getNames());
                        data.setLAST_NAME(seller.getLastName());
                        data.setCELL_PHONE(seller.getCellPhone());
                        data.setEMAIL(seller.getEmail());
                        data.setSALARY(BigDecimal.valueOf(seller.getSalary()));
                        data.setSELLER_USER(seller.getSellerUser());
                        data.setSELLER_PASSWORD(seller.getSellerPassword());

                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(sellerReportData);
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
