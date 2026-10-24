package com.floral.opalina.persistence.jpa.entity;

import com.floral.opalina.domain.enums.EstadoPago;
import com.floral.opalina.domain.enums.EstadoPedido;
import com.floral.opalina.domain.enums.MetodoPago;
import com.floral.opalina.domain.enums.ModalidadEntrega;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "pedido",
    uniqueConstraints = @UniqueConstraint(name = "uq_pedido_codigo", columnNames = "codigo"),
    indexes = @Index(
        name = "ix_pedido_cliente_fecha",
        columnList = "cliente_id, creado_en"
    )
)
public class PedidoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pedido_id")
    private Long id;

    @Column(nullable = false, length = 24)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteJpaEntity cliente;

    @Column(name = "email_contacto", nullable = false, length = 254)
    private String emailContacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega", nullable = false, length = 20)
    private ModalidadEntrega modalidadEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 24)
    private MetodoPago metodoPago;

    @Column(name = "fecha_solicitada", nullable = false)
    private LocalDate fechaSolicitada;

    @Column(name = "hora_solicitada", nullable = false)
    private LocalTime horaSolicitada;

    @Column(name = "costo_delivery", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoDelivery;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DestinatarioJpaEntity destinatario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedidoJpaEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<CambioEstadoPedidoJpaEntity> historial = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public ClienteJpaEntity getCliente() {
        return cliente;
    }

    public void setCliente(ClienteJpaEntity cliente) {
        this.cliente = cliente;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public ModalidadEntrega getModalidadEntrega() {
        return modalidadEntrega;
    }

    public void setModalidadEntrega(ModalidadEntrega modalidadEntrega) {
        this.modalidadEntrega = modalidadEntrega;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDate getFechaSolicitada() {
        return fechaSolicitada;
    }

    public void setFechaSolicitada(LocalDate fechaSolicitada) {
        this.fechaSolicitada = fechaSolicitada;
    }

    public LocalTime getHoraSolicitada() {
        return horaSolicitada;
    }

    public void setHoraSolicitada(LocalTime horaSolicitada) {
        this.horaSolicitada = horaSolicitada;
    }

    public BigDecimal getCostoDelivery() {
        return costoDelivery;
    }

    public void setCostoDelivery(BigDecimal costoDelivery) {
        this.costoDelivery = costoDelivery;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public DestinatarioJpaEntity getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(DestinatarioJpaEntity destinatario) {
        this.destinatario = destinatario;
    }

    public List<ItemPedidoJpaEntity> getItems() {
        return items;
    }

    public List<CambioEstadoPedidoJpaEntity> getHistorial() {
        return historial;
    }

    public PedidoJpaEntity() {
    }
}
