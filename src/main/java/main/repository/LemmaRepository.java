package main.repository;

import main.model.LemmaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query(value = "SELECT l.* FROM lemma as l WHERE l.lemma = ?1 AND l.site_id = ?2 ", nativeQuery = true)
    public Optional<LemmaEntity> findByLemmaSite(String lemma, int site_id);

    @Query(value = "SELECT l.* FROM lemma as l INNER JOIN site as s ON l.site_id = s.id " +
            "WHERE l.lemma IN (?1) AND s.url IN (?2) ",
            nativeQuery = true)
    public List<LemmaEntity> findLemma(List<String> lemmas, List<String> siteUrl);

    @Query(value = "SELECT MAX(frequency) FROM lemma", nativeQuery = true)
    Integer getMaxFrequency();

    @Query(value = "SELECT l.* " +
            "FROM lemma as l " +
            "WHERE l.frequency >= ((100.0 - ?1) / 100.0) * (SELECT MAX(frequency) FROM lemma)",
            countQuery = "SELECT count(l.id) " +
                    "FROM lemma as l " +
                    "WHERE l.frequency >= ((100.0 - ?1) / 100.0) * (SELECT MAX(frequency) FROM lemma)",
            nativeQuery = true)
    List<LemmaEntity> getLemmaFrequencyOverLimit(int percentOver);

    @Modifying
    @Transactional
    @Query(value = "DELETE l FROM lemma as l " +
            "INNER JOIN site as s ON l.site_id = s.id " +
            "WHERE s.url = ?1",
            nativeQuery = true)
    void deleteByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "DELETE l FROM lemma as l " +
            "INNER JOIN site as s ON l.site_id = s.id " +
            "WHERE s.url not in (?1)",
            nativeQuery = true)
    void deleteAllNotIn(List<String> url);
}
