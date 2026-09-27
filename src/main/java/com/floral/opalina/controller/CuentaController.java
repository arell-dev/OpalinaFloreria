package com.floral.opalina.controller;

import com.floral.opalina.dto.PerfilForm;
import com.floral.opalina.exception.RecursoNoEncontradoException;
import com.floral.opalina.model.enums.EstadoPedido;
import com.floral.opalina.service.ClienteService;
import com.floral.opalina.service.PedidoService;
import com.floral.opalina.session.UsuarioSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/cuenta")
public class CuentaController {
    private final ClienteService clientes;
    private final PedidoService pedidos;
    private final UsuarioSession usuario;

    public CuentaController(ClienteService clientes, PedidoService pedidos, UsuarioSession usuario) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.usuario = usuario;
    }

    @GetMapping("/perfil")
    public String perfil(Model model) {
        var cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();
        if (!model.containsAttribute("perfilForm")) {
            PerfilForm form = new PerfilForm();
            String[] nombres = separarNombre(cliente.getNombre());
            form.setNombre(nombres[0]);
            form.setApellidos(nombres[1]);
            form.setTelefono(cliente.getTelefono());
            form.setDireccion(cliente.getDireccion());
            form.setReferencia(cliente.getReferencia());
            model.addAttribute("perfilForm", form);
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("iniciales", obtenerIniciales(cliente.getNombre()));
        return "cuenta/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@Valid @ModelAttribute("perfilForm") PerfilForm form, BindingResult result, Model model, RedirectAttributes redirect) {

        var cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();
        if (result.hasErrors()) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("iniciales", obtenerIniciales(cliente.getNombre()));
            return "cuenta/perfil";
        }

        String nombreCompleto = unirNombre(form.getNombre(), form.getApellidos());

        clientes.actualizar(cliente.getId(), nombreCompleto, form.getTelefono(), form.getDireccion(), form.getReferencia());

        usuario.iniciar(new com.floral.opalina.model.UsuarioActual(cliente.getId(), nombreCompleto, usuario.getUsuario().rol()));

        redirect.addFlashAttribute("mensaje", "Tus datos se actualizaron.");

        return "redirect:/cuenta/perfil";
    }

    @GetMapping("/pedidos")
    public String historial(@RequestParam(required = false) EstadoPedido estado, Model model) {
        var cliente = clientes.porId(usuario.getUsuario().id()).orElseThrow();
        var lista = pedidos.delCliente(usuario.getUsuario().id(), estado);

        model.addAttribute("cliente", cliente);
        model.addAttribute("iniciales", obtenerIniciales(cliente.getNombre()));
        model.addAttribute("pedidos", lista);
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("estadoSeleccionado", estado);
        return "cuenta/pedidos";
    }

    @GetMapping("/pedidos/{id}")
    public String detalle(@PathVariable String id, Model model) {
        var pedido = pedidos.porId(id).filter(p -> p.getClienteId().equals(usuario.getUsuario().id()))
                                      .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el pedido"));

        model.addAttribute("pedido", pedido);
        model.addAttribute("iniciales", obtenerIniciales(pedido.getNombreCliente()));
        return "cuenta/pedido-detalle";
    }

    private String[] separarNombre(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+", 2);
        return new String[] {partes[0], partes.length > 1 ? partes[1] : ""};
    }

    private String unirNombre(String nombres, String apellidos) {
        return Stream.of(nombres.trim(), apellidos.trim())
                     .filter(parte -> !parte.isBlank())
                     .collect(Collectors.joining(" "));
    }

    private String obtenerIniciales(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }
}
