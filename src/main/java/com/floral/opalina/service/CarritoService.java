package com.floral.opalina.service;

import com.floral.opalina.model.Producto;
import com.floral.opalina.session.CarritoSession;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CarritoService {
    private final CarritoSession carrito;
    private final ProductoService productos;
    private final ConfiguracionService configuracion;

    public CarritoService(CarritoSession carrito, ProductoService productos, ConfiguracionService configuracion) {
        this.carrito = carrito;
        this.productos = productos;
        this.configuracion = configuracion;
    }

    public void agregar(String id, int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad mínima es uno");
        }

        Producto producto = buscarProducto(id);
        int actual = carrito.cantidades().getOrDefault(id, 0);
        if (!producto.isComprable() || actual + cantidad > producto.getStock()) {
            throw new IllegalArgumentException("El stock disponible de " + producto.getNombre() + " es " + producto.getStock());
        }
        carrito.agregar(id, cantidad);
    }

    public void cambiar(String id, int cantidad) {
        if (cantidad < 1) {
            quitar(id);
            return;
        }

        Producto producto = buscarProducto(id);
        if (cantidad > producto.getStock()) {
            throw new IllegalArgumentException("El stock disponible de " + producto.getNombre() + " es " + producto.getStock());
        }
        carrito.cantidad(id, cantidad);
    }

    public void ajustar(String id, int ajuste) {
        if (ajuste != -1 && ajuste != 1) {
            throw new IllegalArgumentException("El ajuste debe aumentar o reducir una unidad.");
        }

        int cantidadActual = carrito.cantidades().getOrDefault(id, 0);
        cambiar(id, cantidadActual + ajuste);
    }

    public void quitar(String id) {
        carrito.quitar(id);
    }

    public void vaciar() {
        carrito.vaciar();
    }

    public boolean vacio() {
        return carrito.vacio();
    }

    public List<ItemVista> items() {
        List<ItemVista> resultado = new ArrayList<>();
        carrito.cantidades().forEach((id, cantidad) -> productos.buscarPorId(id).ifPresent(producto -> {
            BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));

            resultado.add(new ItemVista(producto, cantidad, total));
        }));
        return List.copyOf(resultado);
    }

    public BigDecimal subtotal() {
        return items().stream().map(ItemVista::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal delivery(boolean recojo) {
        return calcularDelivery(subtotal(), recojo);
    }

    public BigDecimal calcularDelivery(BigDecimal subtotal, boolean recojo) {
        var reglas = configuracion.obtener();
        if (recojo || subtotal.compareTo(reglas.getDeliveryGratisDesde()) >= 0 || subtotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return reglas.getCostoDelivery();
    }

    public BigDecimal total(boolean recojo) {
        return subtotal().add(delivery(recojo));
    }

    private Producto buscarProducto(String id) {
        return productos.buscarPorId(id)
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    public record ItemVista(Producto producto, int cantidad, BigDecimal total) { }
}
