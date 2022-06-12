package main.repository;

import main.model.IndexEntity;
import main.model.LemmaEntity;
import main.model.PageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    List<IndexEntity> findByPage(PageEntity page);

    void deleteByPage(PageEntity page);

    List<IndexEntity> findByLemma(LemmaEntity lemma);

    @Query(value = "select i.* from indexes as i " +
            "INNER JOIN lemma as l ON l.id = i.lemma_id " +
            "WHERE l.lemma IN :lemmas AND (l.site_id = :siteId OR :siteId IS NULL) " +
            "ORDER BY i.id"
            , countQuery = "select count(i.id) from indexes as i " +
            "INNER JOIN lemma as l ON l.id = i.lemma_id " +
            "WHERE l.lemma IN :lemmas AND (l.site_id = :siteId OR :siteId IS NULL) " +
            "ORDER BY i.id"
            , nativeQuery = true)
    Page<IndexEntity> findByLemmaString(@Param("lemmas") Collection<String> lemmas,
                                        @Param("siteId") Integer siteId,
                                        Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE i FROM indexes as i " +
            "INNER JOIN pages as p ON i.page_id = p.id " +
            "INNER JOIN site as s ON p.site_id = s.id " +
            "WHERE s.url = ?1", nativeQuery = true)
    void deleteByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "DELETE i FROM indexes as i " +
            "INNER JOIN pages as p ON i.page_id = p.id " +
            "INNER JOIN site as s ON p.site_id = s.id " +
            "WHERE s.url not in (?1)", nativeQuery = true)
    void deleteAllNotIn(List<String> url);

}
