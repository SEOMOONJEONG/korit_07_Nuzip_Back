package com.highlight.nuzip.repository;

import com.highlight.nuzip.model.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface MemoRepository extends JpaRepository<Memo, Long> {
    // List<Memo> findByScrapIdOrderByUpdatedAtDesc(Long scrapId);
    @Query("SELECT m FROM Memo m JOIN FETCH m.scrap WHERE m.scrap.id = :scrapId ORDER BY m.updatedAt DESC")
    List<Memo> findByScrapIdWithScrap(@Param("scrapId") Long scrapId);

}