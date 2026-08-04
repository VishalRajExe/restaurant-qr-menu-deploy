package com.restaurantqr.platform.modules.ticket.repository;

import com.restaurantqr.platform.modules.ticket.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    Optional<KnowledgeArticle> findBySlug(String slug);

    @Query("SELECT k FROM KnowledgeArticle k WHERE k.isDeleted = false " +
           "AND (:query IS NULL OR LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeArticle> searchArticles(String query);
}
