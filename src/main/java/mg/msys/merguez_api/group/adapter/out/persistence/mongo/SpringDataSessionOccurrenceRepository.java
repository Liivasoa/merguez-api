package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataSessionOccurrenceRepository extends MongoRepository<SessionOccurrenceDocument, String> {

    boolean existsByGroupIdAndStatus(String groupId, String status);
}
