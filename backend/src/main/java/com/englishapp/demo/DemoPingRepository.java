package com.englishapp.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DemoPingRepository extends JpaRepository<DemoPing, UUID> {
}
