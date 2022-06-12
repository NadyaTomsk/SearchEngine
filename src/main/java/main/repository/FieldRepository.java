package main.repository;

import main.model.FieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldRepository extends JpaRepository<FieldEntity, Integer> {
    @Query("select f from FieldEntity f where f.name = ?1")
    public Optional<FieldEntity> findByName(String name);
}
