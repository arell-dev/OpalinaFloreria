package com.floral.opalina.model;

import java.time.LocalDateTime;

public class Cliente {
    private final String id;
    private final String nombre;
    private final String email;
    private final String telefono;
    private final String direccion;
    private final String referencia;
    private final LocalDateTime creadoEn;

    public Cliente(String id, String nombre, String email, String telefono, String direccion, String referencia, LocalDateTime creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.referencia = referencia;
        this.creadoEn = creadoEn;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public Cliente actualizar(String nombre, String telefono, String direccion, String referencia) {
        return new Cliente(id, nombre, email, telefono, direccion, referencia, creadoEn);
    }
}
