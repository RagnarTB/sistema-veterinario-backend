package com.veterinaria.modelos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import java.time.LocalDate;

@Entity
@Table(name = "examenes_medicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class ExamenMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipoExamen;

    @Column(columnDefinition = "TEXT")
    private String resultados;

    @Column(nullable = false)
    private LocalDate fechaSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;
}
