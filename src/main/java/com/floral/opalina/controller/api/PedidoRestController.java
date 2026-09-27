package com.floral.opalina.controller.api;

import com.floral.opalina.dto.EstadoPedidoForm;
import com.floral.opalina.dto.PedidoApiForm;
import com.floral.opalina.model.Pedido;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoRestController {
    private final PedidoService pedidos;

    public PedidoRestController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    public List<Pedido> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidos.listar(estado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscar(@PathVariable String id) {
        return pedidos.porId(id).map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody PedidoApiForm formulario) {
        Pedido creado = pedidos.crearDesdeApi(formulario);

        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                                                  .buildAndExpand(creado.getId()).toUri();
                                                  
        return ResponseEntity.created(location).body(creado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> cambiarEstado(@PathVariable String id, @Valid @RequestBody EstadoPedidoForm formulario) {
        pedidos.cambiarEstado(id, formulario.getEstado());
        
        return pedidos.porId(id).map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
