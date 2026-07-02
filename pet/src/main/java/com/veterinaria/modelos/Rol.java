package com.veterinaria.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Objects;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardará valores como: "ROLE_ADMIN", "ROLE_CLIENTE", "ROLE_VETERINARIO"
    // Spring Security EXIGE que los roles empiecen con "ROLE_"
    private String nombre;

    private Boolean activo = true;

    // Fecha en la que se modificaron los permisos de este rol por última vez
    @Column(name = "fecha_modificacion_permisos")
    private java.time.LocalDateTime fechaModificacionPermisos;

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.JoinTable(name = "rol_permisos", joinColumns = @jakarta.persistence.JoinColumn(name = "rol_id"), inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "permiso_id"))
    private java.util.Set<Permiso> permisos = new java.util.HashSet<>();

    public Rol(Long id, String nombre, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.activo = activo;
        this.fechaModificacionPermisos = java.time.LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol)) return false;
        Rol rol = (Rol) o;
        return id != null && id.equals(rol.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}