package com.katlehouniversity.ecd.repository;

import com.katlehouniversity.ecd.entity.UploadedStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UploadedStatementRepository extends JpaRepository<UploadedStatement, Long> {

    List<UploadedStatement> findByUploadDateBetween(LocalDateTime start, LocalDateTime end);

    List<UploadedStatement> findByStatus(UploadedStatement.ProcessingStatus status);

    List<UploadedStatement> findByOrderByUploadDateDesc();
}
