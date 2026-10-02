package edu.hcmute.cnpm.cinema.repository;

import edu.hcmute.cnpm.cinema.entity.ConcessionProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcessionProductRepository extends JpaRepository<ConcessionProduct, Long> {
    List<ConcessionProduct> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
