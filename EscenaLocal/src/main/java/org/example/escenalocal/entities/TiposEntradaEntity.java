package org.example.escenalocal.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name="tipos_entrada")
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TiposEntradaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String entrada;

    @OneToMany(mappedBy = "tiposEntrada")
    private Set<EventoTiposEntradaEntity> eventoTipos = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        TiposEntradaEntity that = (TiposEntradaEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

