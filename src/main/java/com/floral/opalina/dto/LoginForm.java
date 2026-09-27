package com.floral.opalina.dto;

import jakarta.validation.constraints.*;

public class LoginForm {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String clave;
    

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}

