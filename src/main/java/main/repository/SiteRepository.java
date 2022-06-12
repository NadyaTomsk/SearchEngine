package main.repository;

import main.model.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    @Query("select s from SiteEntity s where s.url = ?1")
    Optional<SiteEntity> findByUrl(String url);

    @Query("select s from SiteEntity s where s.url like '%:url%'")
    Optional<SiteEntity> findByHost(@Param("url") String url);

    @Query(value = "SELECT CASE WHEN EXISTS(SELECT 1 FROM site WHERE status = 'INDEXING') THEN 1 " +
            "               ELSE 0 end as stat"
            , nativeQuery = true)
    Integer isIndexing();

    @Query(value = "SELECT COUNT(distinct p.id) " +
            "FROM pages as p WHERE p.site_id = ?1",
            nativeQuery = true)
    int getPagesCountById(int id);

    @Query(value = "SELECT COUNT(distinct l.id) " +
            "FROM lemma as l WHERE l.site_id = ?1",
            nativeQuery = true)
    int getLemmasCountById(int id);

    @Modifying
    @Transactional
    @Query(value = "DELETE s FROM site as s WHERE s.url not in (?1)", nativeQuery = true)
    void deleteAllNotIn(List<String> url);

}
