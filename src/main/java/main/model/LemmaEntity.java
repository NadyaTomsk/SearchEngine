package main.model;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLInsert;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "lemma", uniqueConstraints = {@UniqueConstraint(columnNames = {"lemma", "site_id"})})
@SQLInsert(sql = "INSERT INTO lemma (frequency,lemma,site_id, id) VALUES (?,?,?,?) AS new(a,b,d,c) ON DUPLICATE KEY UPDATE frequency = frequency + new.a")
public class LemmaEntity implements Comparable<LemmaEntity> {

    @Id
    @Getter
    private int id;
    @Getter
    @Setter
    @Column(nullable = false)
    private String lemma;
    @Getter
    @Setter
    @Column(nullable = false)
    private int frequency;
    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id")
    private SiteEntity site;

    public LemmaEntity() {
        id = 0;
    }

    public LemmaEntity(String lemma, SiteEntity site) {
        this.lemma = lemma;
        this.site = site;
        id = this.hashCode();
    }

    @Override
    public int compareTo(@NotNull LemmaEntity o) {
        if (this.id == o.id) {
            return 0;
        }
        if (site.getId() != o.site.getId()) {
            return Integer.compare(site.getId(), o.site.getId());
        }
        return lemma.compareTo(o.lemma);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        LemmaEntity that = (LemmaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return site.getId() * 1_000_000 + lemma.hashCode();
    }
}
