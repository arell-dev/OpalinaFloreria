package com.floral.opalina.model;

import java.math.BigDecimal;

public class ConfiguracionTienda {
    private String nombre = "Opalina Florería Piura";
    private String telefono = "+51 939 355 038";
    private String email = "contacto@opalinapiura.com";
    private String direccion = "Av. Vice con Jr. Tambogrande Urb. Santa Ana Mz: M Lote: 5, Piura 20001.";
    private String whatsapp = "51939355038";
    private String zonaDelivery = "Piura urbana";
    private String horarioGeneral = "8:00 AM - 10:00 PM";
    private String instagram = "https://instagram.com/opalina_piura";
    private String facebook = "";
    private String tiktok = "";
    private BigDecimal costoDelivery = new BigDecimal("15.00");
    private BigDecimal deliveryGratisDesde = new BigDecimal("200.00");

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getZonaDelivery() {
        return zonaDelivery;
    }

    public void setZonaDelivery(String zonaDelivery) {
        this.zonaDelivery = zonaDelivery;
    }

    public String getHorarioGeneral() {
        return horarioGeneral;
    }

    public void setHorarioGeneral(String horarioGeneral) {
        this.horarioGeneral = horarioGeneral;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getTiktok() {
        return tiktok;
    }

    public void setTiktok(String tiktok) {
        this.tiktok = tiktok;
    }

    public BigDecimal getCostoDelivery() {
        return costoDelivery;
    }

    public void setCostoDelivery(BigDecimal costoDelivery) {
        this.costoDelivery = costoDelivery;
    }

    public BigDecimal getDeliveryGratisDesde() {
        return deliveryGratisDesde;
    }

    public void setDeliveryGratisDesde(BigDecimal deliveryGratisDesde) {
        this.deliveryGratisDesde = deliveryGratisDesde;
    }
}

