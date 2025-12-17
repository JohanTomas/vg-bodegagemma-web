package pe.edu.vallegrande.sistventas.rest;

import net.sf.jasperreports.engine.*;
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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sistventas.model.reports.ClientReportData;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.service.ClientService;

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
@RequestMapping("/api/v1/clients")
public class ClientRest {
    private final ClientService clientService;
    private static final Logger logger = LoggerFactory.getLogger(ClientRest.class);

    @Autowired
    public ClientRest(ClientService clientService){
        this.clientService = clientService;
    }
    // Endpoint para listar todas las personas
    @GetMapping
    public List<Person> getAllPersons() {
        return this.clientService.getPersons();
    }
    // Endpoint para buscar una persona por número de documento y rol "C"
    @GetMapping("/exists")
    public ResponseEntity<Object> checkIfClientExists(@RequestParam String numberDocument) {
        List<Person> clients = clientService.getExistingClients(numberDocument);
        if (!clients.isEmpty()) {
            return ResponseEntity.ok(clients.get(0)); // Devuelve el primer cliente encontrado
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // Endpoint para listar todos los clientes
    @GetMapping("/list")
    public List<Person> getAllClients() {
        return this.clientService.getClients();
    }

    // Endpoint para listar clientes activos
    @GetMapping("/active")
    public List<Person> getActiveClients() {
        return this.clientService.getActiveClients();
    }

    // Endpoint para listar clientes inactivos
    @GetMapping("/inactive")
    public List<Person> getInactiveClients() {
        return this.clientService.getInactiveClients();
    }

    // Endpoint para obtener un cliente por su ID
    @GetMapping("/{clientId}")
    public ResponseEntity<Object> getClientById(@PathVariable("clientId") Long id) {
        return clientService.getClientById(id);
    }

    //LISTAR PAGINADOR A ACTIVOS CLIENTES
    @GetMapping("/active/pageable")
    public Page<Person> listPageableClientsA(Pageable pageable) {
        return this.clientService.listPageableClientsA(pageable);
    }

    //LISTAR PAGINADOR A INACTIVOS CLIENTES
    @GetMapping("/inactive/pageable")
    public Page<Person> listPageableClientsI(Pageable pageable) {
        return this.clientService.listPageableClientsI(pageable);
    }

    // Ingresar cliente
    @PostMapping
    public ResponseEntity<Object> registrarClient(@RequestBody Person client){
        return this.clientService.newClient(client);
    }

    // Actualizar cliente
    @PutMapping("/{clientId}")
    public ResponseEntity<Object> actualizarClient(@PathVariable("clientId") Long id, @RequestBody Person client){
        return this.clientService.updateClient(id, client);
    }

    // Eliminado fisicamente
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Object> eliminarClient(@PathVariable("clientId") Long id){
        return this.clientService.deleteclient(id);
    }

    // Eliminado Logico
    @PutMapping("/disable/{clientId}")
    public ResponseEntity<Object> disableClientById(@PathVariable("clientId") Long id) {
        return this.clientService.deleteClientById(id);
    }

    // Activar Cliente
    @PutMapping("/activate/{clientId}")
    public ResponseEntity<Object> activateClientById(@PathVariable("clientId") Long id) {
        return this.clientService.activateClientById(id);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_client.jasper");
            // Obtener los datos para el reporte
            List<Person> clients = this.clientService.getActiveClients();
            // Convertir los objetos Person a ClientReportData
            List<ClientReportData> clientReportData = clients.stream()
                    .map(client -> {
                        ClientReportData data = new ClientReportData();
                        data.setID(BigDecimal.valueOf(client.getId()));
                        data.setTYPE_DOCUMENT(client.getTypeDocument());
                        data.setNUMBER_DOCUMENT(client.getNumberDocument());
                        data.setNAMES(client.getNames());
                        data.setLAST_NAME(client.getLastName());
                        data.setCELL_PHONE(client.getCellPhone());
                        data.setEMAIL(client.getEmail());
                        data.setBIRTHDATE(client.getBirthdateFormatted());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(clientReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_client.jasper");
            // Obtener los datos para el reporte
            List<Person> clients = this.clientService.getActiveClients();
            // Convertir los objetos Person a ClientReportData
            List<ClientReportData> clientReportData = clients.stream()
                    .map(client -> {
                        ClientReportData data = new ClientReportData();
                        data.setID(BigDecimal.valueOf(client.getId()));
                        data.setTYPE_DOCUMENT(client.getTypeDocument());
                        data.setNUMBER_DOCUMENT(client.getNumberDocument());
                        data.setNAMES(client.getNames());
                        data.setLAST_NAME(client.getLastName());
                        data.setCELL_PHONE(client.getCellPhone());
                        data.setEMAIL(client.getEmail());
                        data.setBIRTHDATE(client.getBirthdateFormatted());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(clientReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_client.jasper");
            // Obtener los datos para el reporte
            List<Person> clients = this.clientService.getActiveClients();
            // Convertir los objetos Person a ClientReportData
            List<ClientReportData> clientReportData = clients.stream()
                    .map(client -> {
                        ClientReportData data = new ClientReportData();
                        data.setID(BigDecimal.valueOf(client.getId()));
                        data.setTYPE_DOCUMENT(client.getTypeDocument());
                        data.setNUMBER_DOCUMENT(client.getNumberDocument());
                        data.setNAMES(client.getNames());
                        data.setLAST_NAME(client.getLastName());
                        data.setCELL_PHONE(client.getCellPhone());
                        data.setEMAIL(client.getEmail());
                        data.setBIRTHDATE(client.getBirthdateFormatted());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(clientReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_client.jasper");
            // Obtener los datos para el reporte
            List<Person> clients = this.clientService.getActiveClients();
            // Convertir los objetos Person a ClientReportData
            List<ClientReportData> clientReportData = clients.stream()
                    .map(client -> {
                        ClientReportData data = new ClientReportData();
                        data.setID(BigDecimal.valueOf(client.getId()));
                        data.setTYPE_DOCUMENT(client.getTypeDocument());
                        data.setNUMBER_DOCUMENT(client.getNumberDocument());
                        data.setNAMES(client.getNames());
                        data.setLAST_NAME(client.getLastName());
                        data.setCELL_PHONE(client.getCellPhone());
                        data.setEMAIL(client.getEmail());
                        data.setBIRTHDATE(client.getBirthdateFormatted());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(clientReportData);
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




