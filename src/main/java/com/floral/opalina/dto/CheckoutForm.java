package com.floral.opalina.dto;

import com.floral.opalina.model.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public class CheckoutForm {
    @NotBlank(message = "Ingresa el nombre de quien recibirá el pedido.")
    @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.")
    private String nombreReceptor;

    @NotBlank(message = "El correo de tu cuenta es obligatorio.")
    @Email(message = "El correo de tu cuenta no es válido.")
    private String email;

    @NotBlank(message = "Ingresa un teléfono de contacto.")
    @Pattern(
            regexp = "^(\\+?51\\s?)?9[0-9]{2}[\\s-]?[0-9]{3}[\\s-]?[0-9]{3}$",
            message = "Ingresa un número de celular válido.")
    private String telefono;

    @NotNull(message = "Selecciona cómo recibirás el pedido.")
    private ModalidadEntrega modalidad = ModalidadEntrega.DELIVERY;
    private String direccion = "", referencia = "", notas = "";

    @NotNull(message = "Selecciona una fecha de entrega.")
    @Future(message = "Selecciona una fecha a partir de mañana.")
    private LocalDate fecha;

    @NotNull(message = "Selecciona un horario de entrega.")
    private LocalTime hora;
    
    @NotNull(message = "Selecciona un método de pago.")
    private MetodoPago metodoPago;

    public String getNombreReceptor() {
        return nombreReceptor;
    }

    public void setNombreReceptor(String nombreReceptor) {
        this.nombreReceptor = nombreReceptor;
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

    public ModalidadEntrega getModalidad() {
        return modalidad;
    }

    public void setModalidad(ModalidadEntrega modalidad) {
        this.modalidad = modalidad;
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

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }
}

