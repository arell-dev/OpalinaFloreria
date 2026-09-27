package com.floral.opalina.dto;

import jakarta.validation.constraints.*;

public class PerfilForm {
    @NotBlank
    @Size(max = 80)
    private String nombre;

    @NotBlank
    @Size(max = 80)
    private String apellidos;

    @NotBlank
    @Pattern(regexp = "^(\\+?51\\s?)?9[0-9]{2}[\\s-]?[0-9]{3}[\\s-]?[0-9]{3}$", message = "Ingresa un número celular válido.")
    private String telefono;

    private String direccion = "", referencia = "";

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}

