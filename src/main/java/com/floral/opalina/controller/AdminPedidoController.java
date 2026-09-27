package com.floral.opalina.controller;

import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/admin")
public class AdminPedidoController {
    private final PedidoService pedidos;
    private final ClienteService clientes;

    public AdminPedidoController(PedidoService pedidos, ClienteService clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    @GetMapping
    public String inicio() {
        return "redirect:/admin/pedidos";
    }

    @GetMapping("/pedidos")
    public String listar(@RequestParam(required = false) EstadoPedido estado, @RequestParam(required = false) String q, Model model) {
        var pedidosVisibles = pedidos.buscarParaAdministracion(estado, q);
        model.addAttribute("pedidos", pedidosVisibles);
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("consulta", q == null ? "" : q);
        model.addAttribute("totalPedidos", pedidos.contar(null));
        model.addAttribute("pedidosPendientes", pedidos.contar(EstadoPedido.PENDIENTE));
        model.addAttribute("pedidosEnProceso", pedidos.contar(EstadoPedido.EN_PROCESO));
        model.addAttribute("pedidosEnviados", pedidos.contar(EstadoPedido.ENVIADO));
        model.addAttribute("pedidosEntregados", pedidos.contar(EstadoPedido.ENTREGADO));
        model.addAttribute("pedidosCancelados", pedidos.contar(EstadoPedido.CANCELADO));
        model.addAttribute("valorPedidos", pedidos.totalVentas(null));
        return "admin/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String detalle(@PathVariable String id, Model model) {
        var pedido = pedidos.porId(id)
                            .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pedido"));
                            
        model.addAttribute("pedido", pedido);
        model.addAttribute("cliente", clientes.porId(pedido.getClienteId()).orElse(null));
        model.addAttribute("estados", EstadoPedido.values());
        return "admin/pedido-detalle";
    }

    @PostMapping("/pedidos/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam EstadoPedido estado, RedirectAttributes redirect) {
        try {
            pedidos.cambiarEstado(id, estado);
            redirect.addFlashAttribute("mensaje", "Estado del pedido actualizado.");
        } catch (IllegalArgumentException error) {
            redirect.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/pedidos/{id}";
    }
}
