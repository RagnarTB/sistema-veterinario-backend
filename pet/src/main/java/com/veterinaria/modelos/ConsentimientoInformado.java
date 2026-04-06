package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "consentimientos_informados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class ConsentimientoInformado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String textoLegal;

    @Column(nullable = false)
    private Boolean aceptadoPorCliente = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cirugia_id", nullable = false, unique = true)
    private Cirugia cirugia;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
}
