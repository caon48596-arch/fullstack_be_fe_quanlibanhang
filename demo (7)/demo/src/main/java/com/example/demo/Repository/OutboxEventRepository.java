package com.example.demo.Repository;

import com.example.demo.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            String status, LocalDateTime now, Pageable pageable);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);
}
