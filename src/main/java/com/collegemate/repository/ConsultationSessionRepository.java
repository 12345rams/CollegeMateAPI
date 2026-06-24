package com.collegemate.repository;

import com.collegemate.model.ConsultationSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ConsultationSessionRepository extends MongoRepository<ConsultationSession, String> {
    List<ConsultationSession> findBySeekerId(String seekerId);
    List<ConsultationSession> findByAdvisorId(String advisorId);
}
