package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.SupportConversation;
import edu.hcmute.cnpm.cinema.entity.SupportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {

    List<SupportConversation> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    List<SupportConversation> findAllByOrderByUpdatedAtDesc();

    List<SupportConversation> findByStatusOrderByUpdatedAtDesc(SupportStatus status);

    long countByStatus(SupportStatus status);

    @EntityGraph(attributePaths = {"messages", "messages.sender", "customer", "assignedStaff"})
    Optional<SupportConversation> findWithMessagesById(Long id);
}
