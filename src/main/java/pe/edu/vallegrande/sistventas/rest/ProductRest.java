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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.edu.vallegrande.sistventas.model.Product;
import pe.edu.vallegrande.sistventas.model.reports.ProductReportData;
import pe.edu.vallegrande.sistventas.service.ProductService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/v1/products")
public class ProductRest {
    private final ProductService productService;
    private static final Logger logger = LoggerFactory.getLogger(ProductRest.class);
    @Autowired
    public ProductRest(ProductService productService){
        this.productService = productService;
    }
    // Listado completo de productos
    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        return productService.getProducts();
    }
    // Endpoint para buscar un producto por nombre
    @GetMapping("/exists")
    public ResponseEntity<Object> checkIfProductExists(@RequestParam String name) {
        List<Product> product = productService.getExistingProductIgnoreCase(name);
        if (!product.isEmpty()) {
            return ResponseEntity.ok(product.get(0)); // Devuelve el primer producto encontrado
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint para buscar un producto por código
    @GetMapping("/byCode/{code}")
    public ResponseEntity<Object> getProductByCode(@PathVariable String code) {
        return productService.getProductByCode(code);
    }

    //  CODE DE ACTIVE
    // Endpoint para listar productos activos
    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        return productService.getActiveProducts();
    }

    // Endpoint para listar productos inactivos
    @GetMapping("/inactive")
    public ResponseEntity<List<Product>> getInactiveProducts() {
        return productService.getInactiveProducts();
    }

    //listado por ID
    @GetMapping("/{productId}")
    public ResponseEntity<Object> getProductById(@PathVariable("productId") Long id) {
        return this.productService.getProductById(id);
    }

    // Listar paginador de productos activos
    @GetMapping("/active/pageable")
    public ResponseEntity<Page<Product>> listPageableProductsA(Pageable pageable) {
        return productService.listPageableProductsA(pageable);
    }

    // Listar paginador de productos inactivos
    @GetMapping("/inactive/pageable")
    public ResponseEntity<Page<Product>> listPageableProductsI(Pageable pageable) {
        return productService.listPageableProductsI(pageable);
    }
    //Ingresar producto
    @PostMapping
    public ResponseEntity<Object> registrarProduct(@RequestBody Product product){
        ResponseEntity<Object> response = productService.newProduct(product);
        if (response.getStatusCode() == HttpStatus.OK) {
            HashMap<String, Object> responseBody = (HashMap<String, Object>) response.getBody();
            Product savedProduct = (Product) responseBody.get("data");
            URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                    .buildAndExpand(savedProduct.getId()).toUri();
            return ResponseEntity.created(uri).body(responseBody);
        } else {
            return response;
        }
    }
    //actualizar producto por ID
    @PutMapping("/{productId}")
    public ResponseEntity<Object> updateProductById(@PathVariable("productId") Long id, @RequestBody Product updatedProduct) {
        return this.productService.updateProduct(id, updatedProduct);
    }
    //Eliminado fisicamente
    @DeleteMapping("{productId}")
    public ResponseEntity<Object> eliminarProduct(@PathVariable("productId") Long id){
        return this.productService.deleteproduct(id);
    }
    //Eliminado Logico
    @PutMapping("/disable/{productId}")
    public ResponseEntity<Object> disableProductById(@PathVariable("productId") Long id) {
        return this.productService.deleteProductById(id);
    }
    //Activar Producto
    @PutMapping("/activate/{productId}")
    public ResponseEntity<Object> activateProductById(@PathVariable("productId") Long id) {
        return this.productService.activateProductById(id);
    }

    // Listar productos próximos a vencer dentro de 15 días
    @GetMapping("/expiring")
    public ResponseEntity<List<Product>> getExpiringProductsWithin15Days() {
        LocalDate currentDate = LocalDate.now();
        LocalDate expirationDateLimit = currentDate.plusDays(30);
        List<Product> expiringProducts = productService.getExpiringProducts(currentDate, expirationDateLimit);
        return ResponseEntity.ok(expiringProducts);
    }

    // Listar productos con stock menos de 10
    @GetMapping("/lowstock")
    public ResponseEntity<List<Product>> getProductsLowStock() {
        List<Product> lowStockProducts = productService.getProductsLowStock(20);
        return ResponseEntity.ok(lowStockProducts);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_product.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<Product>> responseEntity = this.productService.getActiveProducts();
            List<Product> products = responseEntity.getBody();
            // Formato de fecha deseado
            // Convertir los objetos Product a ProductReportData
            List<ProductReportData> productReportData = products.stream()
                    .map(product -> {
                        ProductReportData data = new ProductReportData();
                        data.setPRODUCT_ID(BigDecimal.valueOf(product.getId()));
                        data.setPRODUCT_NAME(product.getName());
                        data.setCATEGORY_NAME(product.getCategoryProduct().getName()); // Obtener solo el nombre de la categoría
                        data.setPRICE_UNIT(BigDecimal.valueOf(product.getPriceUnit()));
                        data.setUNIT_SALE(product.getUnitSale());
                        // Convertir LocalDate a String utilizando el formato especificado
                        data.setDATE_EXPIRY(product.getDateExpiryFormatted());
                        data.setSTOCK(BigDecimal.valueOf(product.getStock()));
                        data.setACTIVE(product.getActive());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(productReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_product.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<Product>> responseEntity = this.productService.getActiveProducts();
            List<Product> products = responseEntity.getBody();
            // Convertir los objetos Product a ProductReportData
            List<ProductReportData> productReportData = products.stream()
                    .map(product -> {
                        ProductReportData data = new ProductReportData();
                        data.setPRODUCT_ID(BigDecimal.valueOf(product.getId()));
                        data.setPRODUCT_NAME(product.getName());
                        data.setCATEGORY_NAME(product.getCategoryProduct().getName()); // Obtener solo el nombre de la categoría
                        data.setPRICE_UNIT(BigDecimal.valueOf(product.getPriceUnit()));
                        data.setUNIT_SALE(product.getUnitSale());
                        // Convertir LocalDate a String utilizando el formato especificado
                        data.setDATE_EXPIRY(product.getDateExpiryFormatted());
                        data.setSTOCK(BigDecimal.valueOf(product.getStock()));
                        data.setACTIVE(product.getActive());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(productReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_product.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<Product>> responseEntity = this.productService.getActiveProducts();
            List<Product> products = responseEntity.getBody();
            // Convertir los objetos Product a ProductReportData
            List<ProductReportData> productReportData = products.stream()
                    .map(product -> {
                        ProductReportData data = new ProductReportData();
                        data.setPRODUCT_ID(BigDecimal.valueOf(product.getId()));
                        data.setPRODUCT_NAME(product.getName());
                        data.setCATEGORY_NAME(product.getCategoryProduct().getName()); // Obtener solo el nombre de la categoría
                        data.setPRICE_UNIT(BigDecimal.valueOf(product.getPriceUnit()));
                        data.setUNIT_SALE(product.getUnitSale());
                        // Convertir LocalDate a String utilizando el formato especificado
                        data.setDATE_EXPIRY(product.getDateExpiryFormatted());
                        data.setSTOCK(BigDecimal.valueOf(product.getStock()));
                        data.setACTIVE(product.getActive());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(productReportData);
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
            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_product.jasper");
            // Obtener los datos para el reporte
            ResponseEntity<List<Product>> responseEntity = this.productService.getActiveProducts();
            List<Product> products = responseEntity.getBody();
            // Convertir los objetos Product a ProductReportData
            List<ProductReportData> productReportData = products.stream()
                    .map(product -> {
                        ProductReportData data = new ProductReportData();
                        data.setPRODUCT_ID(BigDecimal.valueOf(product.getId()));
                        data.setPRODUCT_NAME(product.getName());
                        data.setCATEGORY_NAME(product.getCategoryProduct().getName()); // Obtener solo el nombre de la categoría
                        data.setPRICE_UNIT(BigDecimal.valueOf(product.getPriceUnit()));
                        data.setUNIT_SALE(product.getUnitSale());
                        // Convertir LocalDate a String utilizando el formato especificado
                        data.setDATE_EXPIRY(product.getDateExpiryFormatted());
                        data.setSTOCK(BigDecimal.valueOf(product.getStock()));
                        data.setACTIVE(product.getActive());
                        return data;
                    })
                    .collect(Collectors.toList());
            // Crear el origen de datos para el reporte
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(productReportData);
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
