package com.floral.opalina.dto;

import jakarta.validation.constraints.*;
import com.floral.opalina.model.Producto;
import java.math.BigDecimal;

public class ProductoForm {
    @NotBlank
    private String nombre;

    @NotBlank
    private String descripcion;

    @NotBlank
    private String categoria;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal precio;

    @Min(0)
    private int stock;
    private boolean disponible = true;
    private boolean destacado;

    @NotBlank
    private String sku;
    
    @NotBlank
    private String imagen;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public boolean isDestacado() {
        return destacado;
    }

    public void setDestacado(boolean destacado) {
        this.destacado = destacado;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Producto toProducto() {
        return new Producto("", nombre, descripcion, categoria, precio, stock, disponible, destacado, sku, imagen);
    }
}

