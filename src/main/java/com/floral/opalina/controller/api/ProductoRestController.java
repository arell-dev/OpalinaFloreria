package com.floral.opalina.controller.api;

import com.floral.opalina.domain.Producto;
import com.floral.opalina.dto.ProductoForm;
import com.floral.opalina.service.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {
    private final ProductoService productoService;

    public ProductoRestController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listar(@RequestParam(required = false) String q, @RequestParam(required = false) String categoria, @RequestParam(required = false) Boolean disponible) {
        return productoService.buscarParaApi(q, categoria, disponible);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscar(@PathVariable String id) {
        return productoService.buscarPorId(id).map(ResponseEntity::ok)
                                              .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoForm formulario) {
        Producto creado = productoService.crear(formulario.toProducto());

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                                  .path("/{id}")
                                                  .buildAndExpand(creado.getId())
                                                  .toUri();
                                                  
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable String id, @Valid @RequestBody ProductoForm formulario) {
        return ResponseEntity.ok(productoService.actualizar(id, formulario.toProducto()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
