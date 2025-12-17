package pe.edu.vallegrande.sistventas.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sistventas.model.CategoryProduct;
import pe.edu.vallegrande.sistventas.model.Person;
import pe.edu.vallegrande.sistventas.repository.CategoryRepo;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    HashMap<String, Object> datos;
    private final CategoryRepo categoryRepo;
    @Autowired
    public CategoryService(CategoryRepo categoryRepo){
        this.categoryRepo = categoryRepo;
    }
    // Listado completo de todas las categorias en orden descendente
    public ResponseEntity<List<CategoryProduct>> getCategories() {
        List<CategoryProduct> categories = this.categoryRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(categories);
    }
    //busqueda por nombre de todas las categorias
    public List<CategoryProduct> getExistingCategoryIgnoreCase(String name) {
        Optional<CategoryProduct> categoryOptional = categoryRepo.findCategoryProductByNameIgnoreCase(name);
        if (categoryOptional.isPresent()) {
            List<CategoryProduct> categoryList = new ArrayList<>();
            categoryList.add(categoryOptional.get());
            return categoryList;
        } else {
            return Collections.emptyList(); // Devuelve una lista vacía si no se encuentra ninguna categoría
        }
    }

    // Listar categorias activos
    public ResponseEntity<List<CategoryProduct>> getActiveCategories() {
        List<CategoryProduct> activeCategories = this.categoryRepo.findByActive("A").stream()
                .sorted(Comparator.comparing(CategoryProduct::getId).reversed()) // Ordenar en orden descendente
                .collect(Collectors.toList());
        return ResponseEntity.ok(activeCategories);
    }

    // Listar categorias inactivos
    public ResponseEntity<List<CategoryProduct>> getInactiveCategories() {
        List<CategoryProduct> inactiveCategories = this.categoryRepo.findByActive("I").stream()
                .sorted(Comparator.comparing(CategoryProduct::getId).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(inactiveCategories);
    }
    // Obtener categoria por ID
    public ResponseEntity<Object> getCategoryById(Long id){
        Optional<CategoryProduct> optionalCategory = categoryRepo.findById(id);
        HashMap<String, Object> responseData = new HashMap<>();
        if (optionalCategory.isPresent()){
            CategoryProduct category = optionalCategory.get();
            responseData.put("data", category);
            return new ResponseEntity<>(responseData, HttpStatus.OK);
        } else {
            responseData.put("error", true);
            responseData.put("mensaje", "No se encontró una categoria con el ID proporcionado.");
            return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
        }
    }
    // Listado con paginador en vendedores activos
    public Page<CategoryProduct> listPageableCategoriesA(Pageable pageable){
        // Obtiene todos los vendedores activos
        List<CategoryProduct> allActiveCategory = this.categoryRepo.findByActive("A");
        // Ordena toda la lista por ID
        allActiveCategory.sort(Comparator.comparing(CategoryProduct::getId).reversed());
        // Aplica la paginación
        List<CategoryProduct> formattedSellers = allActiveCategory.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allActiveCategory.size());
    }

    // Listado con paginador en vendedores inactivos
    public Page<CategoryProduct> listPageableCategoriesI(Pageable pageable){
        // Obtiene todos los vendedores inactivos
        List<CategoryProduct> allInactiveCategories = this.categoryRepo.findByActive("I");
        // Ordena toda la lista por ID
        allInactiveCategories.sort(Comparator.comparing(CategoryProduct::getId).reversed());
        // Aplica la paginación
        List<CategoryProduct> formattedSellers = allInactiveCategories.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(formattedSellers, pageable, allInactiveCategories.size());
    }

    //INGRESAR UNA CATEGORIA
    public ResponseEntity<Object> newCategory(CategoryProduct category) {
        // Establecer el setActive como "A"
        category.setActive("A");
        // Verificar si la categoria ya existe en la base de datos
        Optional<CategoryProduct> existingCategory = categoryRepo.findCategoryProductByName(category.getName());
        if (existingCategory.isPresent()) {
            // Si la categoria ya existe, devolver un mensaje de error
            HashMap<String, Object> responseData = new HashMap<>();
            responseData.put("error", true);
            responseData.put("mensaje", "Ya existe una categoria con ese nombre");
            return new ResponseEntity<>(responseData, HttpStatus.CONFLICT);
        }
        // Si no existe, insertar una nueva categoria
        CategoryProduct savedCategory = categoryRepo.save(category);
        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("mensaje", "Se guardó con éxito");
        responseData.put("data", savedCategory);
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }

    // Actualizar por id
    public ResponseEntity<Object> updateCategory(Long id, CategoryProduct updateCategory){
        Optional<CategoryProduct> optionalCategory = categoryRepo.findById(id);
        HashMap<String, Object> datos = new HashMap<>();
        if (optionalCategory.isPresent()){
            CategoryProduct existingCategory = optionalCategory.get();
            updateExistingCategory(existingCategory, updateCategory);
            // Guardar la categoria actualizado en la base de datos
            categoryRepo.save(existingCategory);
            datos.put("mensaje", "Categoria actualizado con éxito");
            datos.put("data", existingCategory);
            return new ResponseEntity<>(datos, HttpStatus.OK);
        } else {
            datos.put("error", true);
            datos.put("mensaje", "No se encontró un categoria con el ID proporcionado.");
            return new ResponseEntity<>(datos, HttpStatus.NOT_FOUND);
        }
    }
    // Método para actualizar el categoria existente con los datos del categoria actualizado
    private void updateExistingCategory(CategoryProduct existingCategory, CategoryProduct updateCategory) {
        // Campos no modificables o con valores predeterminados
        existingCategory.setName(updateCategory.getName());
        existingCategory.setDescription(updateCategory.getDescription());
    }
    //Eliminar categoria
    public ResponseEntity<Object> deletecategory(Long id){
        datos = new HashMap<>();
        boolean existe = this.categoryRepo.existsById(id);
        if(!existe){
            datos.put("error",true);
            datos.put("mensaje","No existe un categoria con ese id");
            return new ResponseEntity<>(datos, HttpStatus.CONFLICT);
        }
        categoryRepo.deleteById(id);
        datos.put("mensaje","Categoria eliminado");
        return new ResponseEntity<>(datos, HttpStatus.ACCEPTED);
    }
    //Eliminar categoria Logicamente
    public ResponseEntity<Object> deleteCategoryById(Long id) {
        Optional<CategoryProduct> optionalCategory = categoryRepo.findById(id);
        if (optionalCategory.isPresent()) {
            CategoryProduct existingCategory = optionalCategory.get();
            if (existingCategory.getActive().equals("I")) {
                return ResponseEntity.badRequest().body("{\"message\": \"La categoria ya está inactivo.\"}");
            }
            // Cambiar el estado de la categoria a inactivo
            existingCategory.setActive("I");
            // Guardar la categoria actualizado en la base de datos
            categoryRepo.save(existingCategory);
            return ResponseEntity.ok("{\"message\": \"Categoria marcado como inactivo.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //Activar Categoria
    public ResponseEntity<Object> activateCategoryById(Long id) {
        Optional<CategoryProduct> optionalCategory = categoryRepo.findById(id);
        if (optionalCategory.isPresent()) {
            CategoryProduct category = optionalCategory.get();
            if (category.getActive().equals("A")) {
                return ResponseEntity.badRequest().body("{\"message\": \"La Categoria ya está activo.\"}");
            }
            category.setActive("A");
            categoryRepo.save(category);
            return ResponseEntity.ok("{\"message\": \"Categoria activado correctamente.\"}");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    // Count total categories
    public Long countCategories() {
        return categoryRepo.count();
    }
}
