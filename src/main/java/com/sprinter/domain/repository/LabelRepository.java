package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pro entitu {@link Label}.
 */
@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    /** Vrátí štítky projektu (a globální štítky). */
    List<Label> findByProjectIdOrProjectIsNullOrderByNameAsc(Long projectId);

    /** Vrátí globální štítky (bez projektu). */
    List<Label> findByProjectIsNullOrderByNameAsc();
}
