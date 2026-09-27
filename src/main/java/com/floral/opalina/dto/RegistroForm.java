package com.floral.opalina.dto;

import jakarta.validation.constraints.*;

public class RegistroForm {
    @NotBlank
    @Size(min = 3, max = 80)
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(\\+?51\\s?)?9[0-9]{2}[\\s-]?[0-9]{3}[\\s-]?[0-9]{3}$", message = "Ingresa un número celular válido.")
    private String telefono;

    @NotBlank
    @Size(min = 8, max = 100)
    private String clave;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}

