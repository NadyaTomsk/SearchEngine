package main.repository;

import main.model.PageEntity;
import org.springframework.context.annotation.Primary;
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
import java.util.Optional;

@Primary
@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Query(value = "SELECT p.id, \n" +
            "\tp.url_path,\n" +
            "\tp.content,\n" +
            "\tp.code,\n" +
            "\tp.site_id,\n" +
            "\tsum(i.index_rank) / d.i_rank as percent_rank,\n" +
            "\tGROUP_CONCAT(l.lemma) as lemma_list \n" +
            "from pages p \n" +
            "inner join site as s \n" +
            "\ton s.id = p.site_id \n" +
            "inner join indexes as i \n" +
            "\ton i.page_id = p.id \n" +
            "INNER JOIN lemma as l \n" +
            "\tON l.id = i.lemma_id \n" +
            "\tand l.lemma IN :lemmas \n" +
            ", (SELECT \n" +
            "\t\tsum(i.index_rank) as i_rank\n" +
            "\tfrom pages p \n" +
            "\tinner join site as s \n" +
            "\t\ton s.id = p.site_id \n" +
            "\tinner join indexes as i \n" +
            "\t\ton i.page_id = p.id \n" +
            "\tINNER JOIN lemma as l \n" +
            "\t\tON l.id = i.lemma_id \n" +
            "\t\tand l.lemma IN :lemmas \n" +
            "\tWHERE p.code = 200 AND s.url IN :sites \n" +
            "\tGROUP BY p.id , \n" +
            "\t\tp.code , \n" +
            "\t\tp.url_path , \n" +
            "\t\tp.site_id , \n" +
            "\t\tp.content\n" +
            "\tORDER BY count(l.id) DESC, MAX(l.frequency),percent_rank DESC, p.id\n" +
            "\tlimit 1)  as d\n" +
            "WHERE p.code = 200 AND s.url IN :sites \n" +
            "GROUP BY p.id , \n" +
            "\tp.code , \n" +
            "\tp.url_path , \n" +
            "\tp.site_id , \n" +
            "\tp.content\n" +
            "ORDER BY count(l.id) DESC, MAX(l.frequency), percent_rank DESC, p.id"
            , countQuery = "SELECT count(p.id)  \n" +
            "from pages p \n" +
            "inner join site as s \n" +
            "\ton s.id = p.site_id \n" +
            "inner join indexes as i \n" +
            "\ton i.page_id = p.id \n" +
            "INNER JOIN lemma as l \n" +
            "\tON l.id = i.lemma_id \n" +
            "\tand l.lemma IN :lemmas \n" +
            ", (SELECT \n" +
            "\t\tsum(i.index_rank) as i_rank\n" +
            "\tfrom pages p \n" +
            "\tinner join site as s \n" +
            "\t\ton s.id = p.site_id \n" +
            "\tinner join indexes as i \n" +
            "\t\ton i.page_id = p.id \n" +
            "\tINNER JOIN lemma as l \n" +
            "\t\tON l.id = i.lemma_id \n" +
            "\t\tand l.lemma IN :lemmas \n" +
            "\tWHERE p.code = 200 AND s.url IN :sites \n" +
            "\tGROUP BY p.id , \n" +
            "\t\tp.code , \n" +
            "\t\tp.url_path , \n" +
            "\t\tp.site_id , \n" +
            "\t\tp.content\n" +
            "\tORDER BY count(l.id) DESC, MAX(l.frequency), percent_rank DESC, p.id\n" +
            "\tlimit 1)  as d\n" +
            "WHERE p.code = 200 AND s.url IN :sites \n" +
            "GROUP BY p.id , \n" +
            "\tp.code , \n" +
            "\tp.url_path , \n" +
            "\tp.site_id , \n" +
            "\tp.content\n" +
            "ORDER BY count(l.id) DESC, MAX(l.frequency), percent_rank DESC, p.id"
            , nativeQuery = true
    )
    Page<PageEntity> getSearchResult(@Param("lemmas") Collection<String> lemmas,
                                     @Param("sites") List<String> sites,
                                     Pageable pageable);


    @Query(value = "select EXISTS(SELECT 1 FROM indexes as i\n" +
            "                INNER JOIN lemma as l\n" +
            "                 ON l.id = i.lemma_id\n" +
            "                AND l.lemma = :lemmaString\n" +
            "             WHERE  i.page_id = p.id)\n" +
            "from pages as p\n" +
            "WHERE p.id= :id"
            , nativeQuery = true
    )
    int isExistsLemmaForPage(@Param("id") int id, @Param("lemmaString") String lemma);

    @Transactional
    @Modifying
    default int saveAndUpdate(PageEntity page) {
        Optional<PageEntity> entityOpt = findByPath(page.getPath(), page.getSite().getId());
        if (entityOpt.isPresent()) {
            PageEntity update = entityOpt.get();
            if (update.getCode() == 200) {
                page = update;
                return page.getId();
            }
            update.setCode(page.getCode());
            update.setContent(page.getContent());
            page = save(update);
            return page.getId();
        } else {
            page = save(page);
            return page.getId();
        }
    }

    @Query(value = "SELECT p.* FROM pages as p WHERE p.url_path = ?1 AND p.site_id = ?2 ", nativeQuery = true)
    Optional<PageEntity> findByPath(String path, int site_id);

    @Query(value = "select distinct p.* from pages as p " +
            "INNER JOIN indexes as i ON i.page_id = p.id " +
            "INNER JOIN lemma as l ON l.id = i.lemma_id " +
            "WHERE l.lemma = :?1"
            , countQuery = "select count(distinct p.id) from pages as p " +
            "INNER JOIN indexes as i ON i.page_id = p.id " +
            "INNER JOIN lemma as l ON l.id = i.lemma_id " +
            "WHERE l.lemma = :?1"
            , nativeQuery = true)
    List<PageEntity> findByLemma(String lemma);

    @Modifying
    @Transactional
    @Query(value = "DELETE p FROM pages as p " +
            "INNER JOIN site as s " +
            "ON p.site_id = s.id " +
            "WHERE s.url = ?1", nativeQuery = true)
    void deleteByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "DELETE p FROM pages as p " +
            "INNER JOIN site as s " +
            "ON p.site_id = s.id " +
            "WHERE s.url not in (?1)", nativeQuery = true)
    void deleteAllNotIn(List<String> url);

}
