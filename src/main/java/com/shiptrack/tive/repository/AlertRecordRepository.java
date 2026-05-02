package com.shiptrack.tive.repository;

import com.shiptrack.tive.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, String> {
}

