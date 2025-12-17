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
import pe.edu.vallegrande.sistventas.model.CategoryProduct;
import pe.edu.vallegrande.sistventas.model.reports.CategoryReportData;
import pe.edu.vallegrande.sistventas.service.CategoryService;

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
@RequestMapping("/api/v1/categories")
public class CategoryRest {
    private final CategoryService categoryService;
    private static final Logger logger = LoggerFactory.getLogger(CategoryRest.class);

    @Autowired
    public CategoryRest(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    // Endpoint para obtener el recuento total de categorías
    @GetMapping("/count")
    public Long countCategories() {
        return categoryService.countCategories();
    }

    // Endpoint para listar todas las categorias
    @GetMapping
    public ResponseEntity<List<CategoryProduct>> getAllCategories() {
        return categoryService.getCategories();
    }
    // Endpoint para buscar una persona por número de documento y rol "V"
    @GetMapping("/exists")
    public ResponseEntity<Object> checkIfCategoryExists(@RequestParam String name) {
        List<CategoryProduct> category = categoryService.getExistingCategoryIgnoreCase(name);
        if (category != null) {
            return ResponseEntity.ok(category.get(0)); // Devuelve el primer cliente encontrado
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //  CODE DE ACTIVE
    // Endpoint para listar categorias activos
    @GetMapping("/active")
    public ResponseEntity<List<CategoryProduct>> getActiveCategories() {
        return categoryService.getActiveCategories();
    }

    // Endpoint para listar categorias inactivos
    @GetMapping("/inactive")
    public ResponseEntity<List<CategoryProduct>> getInactiveCategories() {
        return categoryService.getInactiveCategories();
    }

    // Endpoint para obtener un cliente por su ID
    @GetMapping("/{categoryId}")
    public ResponseEntity<Object> getCategoryById(@PathVariable("categoryId") Long id) {
        return categoryService.getCategoryById(id);
    }

    // Endpoint para listar categorías activas con paginación
    @GetMapping("/active/pageable")
    public Page<CategoryProduct> listPageableCategoriesA(Pageable pageable) {
        return categoryService.listPageableCategoriesA(pageable);
    }

    // Endpoint para listar categorías inactivas con paginación
    @GetMapping("/inactive/pageable")
    public Page<CategoryProduct> listPageableCategoriesI(Pageable pageable) {
        return categoryService.listPageableCategoriesI(pageable);
    }

    // Ingresar cliente
    @PostMapping
    public ResponseEntity<Object> registrarCategory(@RequestBody CategoryProduct category){
        return this.categoryService.newCategory(category);
    }

    // Actualizar cliente
    @PutMapping("/{categoryId}")
    public ResponseEntity<Object> updateCategory(@PathVariable("categoryId") Long id, @RequestBody CategoryProduct category){
        return this.categoryService.updateCategory(id, category);
    }

    // Eliminado fisicamente
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Object> deleteCategory(@PathVariable("categoryId") Long id){
        return this.categoryService.deletecategory(id);
    }

    // Eliminado Logico
    @PutMapping("/disable/{categoryId}")
    public ResponseEntity<Object> disableCategoryById(@PathVariable("categoryId") Long id) {
        return this.categoryService.deleteCategoryById(id);
    }

    // Activar Cliente
    @PutMapping("/activate/{categoryId}")
    public ResponseEntity<Object> activateCategoryById(@PathVariable("categoryId") Long id) {
        return this.categoryService.activateCategoryById(id);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_category.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<CategoryProduct>> responseEntity = this.categoryService.getActiveCategories();
            List<CategoryProduct> categories = responseEntity.getBody();
            // Convertir los objetos Person a ClientReportData
            List<CategoryReportData> categoryReportData = categories.stream()
                    .map(category -> {
                        CategoryReportData data = new CategoryReportData();
                        data.setID(BigDecimal.valueOf(category.getId()));
                        data.setNAME(category.getName());
                        data.setDESCRIPTION(category.getDescription());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(categoryReportData);
            // Llenar el reporte con los datos
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);
            // Exportar el reporte a un array de bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);
            // Establecer los encabezados de la respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("category_report.pdf").build());
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_category.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<CategoryProduct>> responseEntity = this.categoryService.getActiveCategories();
            List<CategoryProduct> categories = responseEntity.getBody();
            // Convertir los objetos Person a ClientReportData
            List<CategoryReportData> categoryReportData = categories.stream()
                    .map(category -> {
                        CategoryReportData data = new CategoryReportData();
                        data.setID(BigDecimal.valueOf(category.getId()));
                        data.setNAME(category.getName());
                        data.setDESCRIPTION(category.getDescription());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(categoryReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_category.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<CategoryProduct>> responseEntity = this.categoryService.getInactiveCategories();
            List<CategoryProduct> categories = responseEntity.getBody();
            // Convertir los objetos Person a ClientReportData
            List<CategoryReportData> categoryReportData = categories.stream()
                    .map(category -> {
                        CategoryReportData data = new CategoryReportData();
                        data.setID(BigDecimal.valueOf(category.getId()));
                        data.setNAME(category.getName());
                        data.setDESCRIPTION(category.getDescription());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(categoryReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_category.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<CategoryProduct>> responseEntity = this.categoryService.getInactiveCategories();
            List<CategoryProduct> categories = responseEntity.getBody();
            // Convertir los objetos Person a ClientReportData
            List<CategoryReportData> categoryReportData = categories.stream()
                    .map(category -> {
                        CategoryReportData data = new CategoryReportData();
                        data.setID(BigDecimal.valueOf(category.getId()));
                        data.setNAME(category.getName());
                        data.setDESCRIPTION(category.getDescription());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(categoryReportData);
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
