package com.lms.system.repository;

import com.lms.system.model.DirectConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {

    @EntityGraph(attributePaths = {"firstUser", "secondUser"})
    Optional<DirectConversation> findByFirstUser_IdAndSecondUser_Id(Long firstUserId, Long secondUserId);

    @EntityGraph(attributePaths = {"firstUser", "secondUser"})
    Page<DirectConversation> findByFirstUser_IdOrSecondUser_Id(
            Long firstUserId,
            Long secondUserId,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"firstUser", "secondUser"})
    Optional<DirectConversation> findById(Long id);
}
