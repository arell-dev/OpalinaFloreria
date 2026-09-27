package com.floral.opalina.controller.api;

import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.model.Cliente;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.PedidoService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {
    private final ClienteService clientes;
    private final PedidoService pedidos;

    public ClienteRestController(ClienteService clientes, PedidoService pedidos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clientes.listar().stream().filter(c -> !c.getId().equals("admin-opalina")).toList();
    }

    @GetMapping("/{id}")
    public ResumenCliente buscar(@PathVariable String id) {
        Cliente cliente = clientes.porId(id)
                                  .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente"));
                                  
        var compras = pedidos.delCliente(id);
        BigDecimal totalGastado = compras.stream().map(p -> p.getTotal())
                                                  .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenCliente(cliente, compras.size(), totalGastado);
    }

    public record ResumenCliente(Cliente cliente, int cantidadPedidos, BigDecimal totalGastado) { }
}
