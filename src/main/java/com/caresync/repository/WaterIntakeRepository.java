package com.caresync.repository;

import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {

    List<WaterIntake> findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
            User user, LocalDateTime startTime, LocalDateTime endTime);
}
