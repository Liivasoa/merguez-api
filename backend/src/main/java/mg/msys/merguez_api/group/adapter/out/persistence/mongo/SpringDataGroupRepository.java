package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataGroupRepository extends MongoRepository<GroupDocument, String> {

    List<GroupDocument> findAllByMembersUserId(String userId);
}
