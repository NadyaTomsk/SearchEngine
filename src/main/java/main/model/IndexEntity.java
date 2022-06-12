package main.model;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLInsert;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "indexes", uniqueConstraints = {@UniqueConstraint(columnNames = {"page_id", "lemma_id"})})
@SQLInsert(sql = "INSERT INTO indexes (lemma_id, page_id, index_rank, id) VALUES (?,?,?,?) as new(a,b,c,d) ON DUPLICATE KEY UPDATE index_rank = index_rank + new.c")
public class IndexEntity implements Comparable<IndexEntity> {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @Getter
    @Setter
    @JoinColumn(nullable = false, name = "page_id", referencedColumnName = "id")
    private PageEntity page;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @Getter
    @Setter
    @JoinColumn(nullable = false, name = "lemma_id", referencedColumnName = "id")
    private LemmaEntity lemma;

    @Getter
    @Setter
    @Column(nullable = false, name = "index_rank")
    private float rank;

    @Override
    public int compareTo(@NotNull IndexEntity o) {
        if (o.id != 0 && o.id == this.id) {
            return 0;
        }
        if (getPage().getId() == o.getPage().getId()) {
            return lemma.compareTo(o.lemma);
        }
        return page.compareTo(o.page);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        IndexEntity that = (IndexEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return page.getId() * 1_000_000 + lemma.getId();
    }
}
