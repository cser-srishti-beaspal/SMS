package com.stationery.request.repository;

import com.stationery.request.model.RequestStatus;
import com.stationery.request.model.StationeryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<StationeryRequest, Long> {

    List<StationeryRequest> findByStudentUsername(String username);

    List<StationeryRequest> findByStatus(RequestStatus status);

    List<StationeryRequest> findByStudentUsernameAndStatus(String username, RequestStatus status);

    Optional<StationeryRequest> findByRequestId(String requestId);

    Page<StationeryRequest> findByStudentUsername(String username, Pageable pageable);
}
