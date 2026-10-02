package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
}
