package mg.msys.merguez_api.infrastructure.group;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataGroupRepository extends MongoRepository<GroupDocument, String> {
}
