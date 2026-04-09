package com.codehaja.domain.generation.repository;

import com.codehaja.domain.generation.entity.PromptTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateVersionRepository extends JpaRepository<PromptTemplateVersion, Long> {

    /**
     * Find the active version for a template by template name.
     * This is the primary lookup used by PromptContentProvider.
     */
    @Query("SELECT v FROM PromptTemplateVersion v JOIN v.template t " +
           "WHERE t.name = :templateName AND v.isActive = true")
    Optional<PromptTemplateVersion> findActiveByTemplateName(@Param("templateName") String templateName);

    List<PromptTemplateVersion> findByTemplateIdOrderByVersionNumberDesc(Long templateId);

    Optional<PromptTemplateVersion> findByTemplateIdAndIsActiveTrue(Long templateId);
}
