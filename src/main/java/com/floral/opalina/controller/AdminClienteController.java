package com.floral.opalina.controller;

import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Locale;

@Controller
@RequestMapping("/admin/clientes")
public class AdminClienteController {
    private final ClienteService clientes;
    private final PedidoService pedidos;

    public AdminClienteController(ClienteService clientes, PedidoService pedidos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, @RequestParam(required = false, defaultValue = "nombre") String ordenar, Model model) {

        var resumenes = clientes.listar().stream()
                                         .filter(cliente -> !cliente.getId().equals("admin-opalina"))
                                         .map(cliente -> {

            var compras = pedidos.delCliente(cliente.getId());
            var gastado = compras.stream().map(p -> p.getTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);

            return new ResumenCliente(cliente, compras.size(), gastado);

        }).toList();

        var consulta = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        var visibles = resumenes.stream()
                                .filter(resumen -> consulta.isEmpty() || resumen.cliente().getNombre().toLowerCase(Locale.ROOT).contains(consulta) || resumen.cliente().getEmail().toLowerCase(Locale.ROOT).contains(consulta) || resumen.cliente().getTelefono().toLowerCase(Locale.ROOT).contains(consulta))
                                
                                .sorted(comparador(ordenar))
                                .toList();
                
        int totalPedidos = resumenes.stream().mapToInt(ResumenCliente::pedidos).sum();

        BigDecimal totalGastado = resumenes.stream()
                                           .map(ResumenCliente::totalGastado)
                                           .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal promedio = totalPedidos == 0 ? BigDecimal.ZERO : totalGastado.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);

        model.addAttribute("clientes", visibles);
        model.addAttribute("consulta", q == null ? "" : q);
        model.addAttribute("ordenSeleccionado", ordenar);
        model.addAttribute("totalClientes", resumenes.size());
        model.addAttribute("pedidosVinculados", totalPedidos);
        model.addAttribute("totalGastado", totalGastado);
        model.addAttribute("promedioPedido", promedio);
        return "admin/clientes";
    }

    private Comparator<ResumenCliente> comparador(String orden) {
        return switch (orden) {
            case "pedidos" -> Comparator.comparingInt(ResumenCliente::pedidos).reversed()
                                        .thenComparing(resumen -> resumen.cliente().getNombre());
            case "total" -> Comparator.comparing(ResumenCliente::totalGastado).reversed()
                                      .thenComparing(resumen -> resumen.cliente().getNombre());
            default -> Comparator.comparing(resumen -> resumen.cliente().getNombre());
        };
    }

    public record ResumenCliente(com.floral.opalina.model.Cliente cliente, int pedidos, BigDecimal totalGastado) { }
}
