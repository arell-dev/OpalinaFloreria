package com.floral.opalina.model;

import java.math.BigDecimal;

public class Producto {
    private final String id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String sku;
    private String imagen;
    private BigDecimal precio;
    private int stock;
    private boolean disponible;
    private boolean destacado;

    public Producto(String id, String nombre, String descripcion, String categoria, BigDecimal precio, int stock, boolean disponible, boolean destacado, String sku, String imagen) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
        this.disponible = disponible;
        this.destacado = destacado;
        this.sku = sku;
        this.imagen = imagen;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getStock() {
        return stock;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public boolean isComprable() {
        return disponible && stock > 0;
    }

    public boolean isDestacado() {
        return destacado;
    }

    public String getSku() {
        return sku;
    }

    public String getImagen() {
        return imagen;
    }

    public void actualizar(String nombre, String descripcion, String categoria, BigDecimal precio, int stock, boolean disponible, boolean destacado, String sku, String imagen) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
        this.disponible = disponible;
        this.destacado = destacado;
        this.sku = sku;
        this.imagen = imagen;
    }

    public synchronized void descontar(int cantidad) {
        if (cantidad < 1 || cantidad > stock) {
            throw new IllegalArgumentException("Stock insuficiente de " + nombre);
        }
        stock -= cantidad;
    }

    public synchronized void reponer(int cantidad) {
        stock += cantidad;
    }
}
