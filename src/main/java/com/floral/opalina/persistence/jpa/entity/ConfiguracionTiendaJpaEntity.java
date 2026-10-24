package com.floral.opalina.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_tienda")
public class ConfiguracionTiendaJpaEntity {
    @Id
    @Column(name = "configuracion_id")
    private Byte id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 180)
    private String direccion;

    @Column(nullable = false, length = 20)
    private String whatsapp;

    @Column(name = "zona_delivery", nullable = false, length = 200)
    private String zonaDelivery;

    @Column(name = "horario_general", nullable = false, length = 200)
    private String horarioGeneral;

    @Column(length = 255)
    private String instagram;

    @Column(length = 255)
    private String facebook;

    @Column(length = 255)
    private String tiktok;

    @Column(name = "costo_delivery", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoDelivery;

    @Column(name = "delivery_gratis_desde", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryGratisDesde;

    @Column(name = "ultimo_numero_pedido", nullable = false)
    private long ultimoNumeroPedido;

    public Byte getId() {
        return id;
    }

    public void setId(Byte id) {
        this.id = id;
    }

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

    public long getUltimoNumeroPedido() {
        return ultimoNumeroPedido;
    }

    public void setUltimoNumeroPedido(long ultimoNumeroPedido) {
        this.ultimoNumeroPedido = ultimoNumeroPedido;
    }

    public ConfiguracionTiendaJpaEntity() {
    }
}
