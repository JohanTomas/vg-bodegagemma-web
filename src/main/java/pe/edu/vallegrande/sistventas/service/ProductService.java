package pe.edu.vallegrande.sistventas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sistventas.model.Product;
import pe.edu.vallegrande.sistventas.model.Supplier;
import pe.edu.vallegrande.sistventas.repository.ProductRepo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    HashMap<String, Object> datos;
    private final ProductRepo productRepo;

    @Autowired
    public ProductService(ProductRepo productRepo){
        this.productRepo = productRepo;
    }
    // Método para formatear la fecha de expiracion
    private Product formatDateOfDateExpiry(Product product) {
        if (product != null && product.getDateExpiry() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            String formattedDate = product.getDateExpiry().format(formatter);
            formattedDate = formattedDate.replace(".", ""); // Eliminar el punto después del mes
            product.setDateExpiryFormatted(formattedDate);
        }
        return product;
    }

    // Listado completo de productos
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> products = productRepo.findAll();
        return ResponseEntity.ok(products); // Retornar la lista con status 200 OK
    }
    // Método para buscar un producto por nombre sin distinguir mayúsculas y minúsculas
    public List<Product> getExistingProductIgnoreCase(String name) {
        Optional<Product> productOptional = productRepo.findByNameIgnoreCase(name);
        if (productOptional.isPresent()) {
            return Collections.singletonList(productOptional.get());
        } else {
            return Collections.emptyList(); // Devuelve una lista vacía si no se encuentra ningún producto
        }
    }
    // Método para buscar un producto por código
    public ResponseEntity<Object> getProductByCode(String code) {
        Optional<Product> optionalProduct = productRepo.findByCode(code);
        datos = new HashMap<>();
        if (optionalProduct.isPresent()) {
            datos.put("data", optionalProduct.get());
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un producto con el código proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }

    // Listar productos activos
    public ResponseEntity<List<Product>> getActiveProducts() {
        List<Product> activeProducts = productRepo.findByActive("A").stream()
                .map(this::formatDateOfDateExpiry)
                .sorted(Comparator.comparing(Product::getId).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(activeProducts);
    }

    // Listar productos inactivos
    public ResponseEntity<List<Product>> getInactiveProducts() {
        List<Product> inactiveProducts = productRepo.findByActive("I").stream()
                .map(this::formatDateOfDateExpiry)
                .sorted(Comparator.comparing(Product::getId).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(inactiveProducts);
    }

    //Listado por id
    public ResponseEntity<Object> getProductById(Long id) {
        Optional<Product> optionalProduct = productRepo.findById(id);
        datos = new HashMap<>();
        if (optionalProduct.isPresent()) {
            datos.put("data", optionalProduct.get());
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un producto con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Listado con paginador en productos activos
    public ResponseEntity<Page<Product>> listPageableProductsA(Pageable pageable){
        List<Product> productsPage = this.productRepo.findByActive("A");
        productsPage.sort(Comparator.comparing(Product::getId).reversed());
        List<Product> formattedProducts = productsPage.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::formatDateOfDateExpiry)
                .collect(Collectors.toList());
        Page<Product> newPage = new PageImpl<>(formattedProducts, pageable, productsPage.size());
        return ResponseEntity.ok(newPage);
    }

    // Listado con paginador en productos inactivos
    public ResponseEntity<Page<Product>> listPageableProductsI(Pageable pageable){
        List<Product> productsPage = this.productRepo.findByActive("I");
        productsPage.sort(Comparator.comparing(Product::getId).reversed());
        List<Product> formattedProducts = productsPage.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::formatDateOfDateExpiry)
                .collect(Collectors.toList());
        Page<Product> newPage = new PageImpl<>(formattedProducts, pageable, productsPage.size());
        return ResponseEntity.ok(newPage);
    }
    // Ingresar producto
    public ResponseEntity<Object> newProduct(Product product) {
        product.setActive("A"); // Establecer el estado activo por defecto
        Optional<Product> existingProduct = productRepo.findProductByName(product.getName());
        if (existingProduct.isPresent()) {
            HashMap<String, Object> responseData = new HashMap<>();
            responseData.put("error", true);
            responseData.put("mensaje", "Ya existe un producto con ese nombre");
            return new ResponseEntity<>(responseData, HttpStatus.CONFLICT);
        }
        Product savedProduct = productRepo.save(product);
        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("mensaje", "Producto guardado con éxito");
        responseData.put("data", savedProduct);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    // Actualizar por código
    public ResponseEntity<Object> updateProduct(Long id, Product updatedProduct) {
        Optional<Product> optionalProduct = productRepo.findById(id);
        HashMap<String, Object> responseData = new HashMap<>();
        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            updateExistingProduct(existingProduct, updatedProduct); // Actualizar el producto existente con los datos del producto actualizado
            productRepo.save(existingProduct); // Guardar el producto actualizado en la base de datos
            responseData.put("mensaje", "Producto actualizado con éxito");
            responseData.put("data", existingProduct);
            return new ResponseEntity<>(responseData, HttpStatus.OK);
        } else {
            responseData.put("error", true);
            responseData.put("mensaje", "No se encontró un producto con el código proporcionado.");
            return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
        }
    }

    // Método para actualizar el producto existente con los datos del producto actualizado
    private void updateExistingProduct(Product existingProduct, Product updatedProduct) {
        existingProduct.setCode(updatedProduct.getCode());
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setCategoryProduct(updatedProduct.getCategoryProduct());
        existingProduct.setPriceUnit(updatedProduct.getPriceUnit());
        existingProduct.setUnitSale(updatedProduct.getUnitSale());
        existingProduct.setDateExpiry(updatedProduct.getDateExpiry());
        existingProduct.setStock(updatedProduct.getStock());
    }

    //Eliminar producto
    public ResponseEntity<Object> deleteproduct(Long id){
        datos = new HashMap<>();
        boolean existe = this.productRepo.existsById(id);
        if(!existe){
            datos.put("error",true);
            datos.put("messaje","No existe un producto con ese id");
            return new ResponseEntity<>(datos, HttpStatus.CONFLICT);
        }
        productRepo.deleteById(id);
        datos.put("messaje","Producto eliminado");
        return new ResponseEntity<>(datos, HttpStatus.ACCEPTED);
    }
    //Eliminar producto Logicamente
    public ResponseEntity<Object> deleteProductById(Long id) {
        Optional<Product> optionalProduct = productRepo.findById(id);
        datos = new HashMap<>();
        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            if (existingProduct.getActive().equals("I")) {
                datos.put("error", true);
                datos.put("message", "El producto ya está inactivo.");
                return new ResponseEntity<>(datos, HttpStatus.BAD_REQUEST);
            }
            // Cambiar el estado del producto a inactivo
            existingProduct.setActive("I");
            // Guardar el producto actualizado en la base de datos
            productRepo.save(existingProduct);
            datos.put("message", "Producto marcado como inactivo.");
            datos.put("data", existingProduct);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("message", "No se encontró un producto con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    //Activar Producto
    public ResponseEntity<Object> activateProductById(Long id) {
        Optional<Product> optionalProduct = productRepo.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            if (product.getActive().equals("A")) {
                return ResponseEntity.badRequest().body("{\"message\": \"El Producto ya está activo.\"}");
            }
            product.setActive("A");
            productRepo.save(product);
            return ResponseEntity.ok("{\"message\": \"Producto activado correctamente.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Obtener productos próximos a vencer dentro de 15 días
    public List<Product> getExpiringProducts(LocalDate currentDate, LocalDate expirationDateLimit) {
        return productRepo.findByDateExpiryBetween(currentDate, expirationDateLimit)
                .stream()
                .map(this::formatDateOfDateExpiry)
                .collect(Collectors.toList());
    }

    // Obtener productos con stock menos de un valor dado
    public List<Product> getProductsLowStock(int stockLimit) {
        return productRepo.findByStockLessThan(stockLimit)
                .stream()
                .map(this::formatDateOfDateExpiry)
                .collect(Collectors.toList());
    }
}