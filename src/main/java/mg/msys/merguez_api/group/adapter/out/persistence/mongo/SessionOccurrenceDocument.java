package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@Document("session-occurrences")
@CompoundIndexes({
    @CompoundIndex(
            name = "active_occurrence_per_group",
            def = "{ 'groupId': 1 }",
            unique = true,
            partialFilter = "{ 'status': 'IN_PROGRESS' }"),
    @CompoundIndex(
            name = "group_occurrence_history",
            def = "{ 'groupId': 1, 'startedAt': -1 }")
})
record SessionOccurrenceDocument(
        @Id String id, String groupId, String scheduleId, String status, Date startedAt) {

    static SessionOccurrenceDocument from(SessionOccurrence occurrence) {
        return new SessionOccurrenceDocument(
                UUID.randomUUID().toString(),
                occurrence.groupId().value().toString(),
                occurrence.scheduleId().toString(),
                "IN_PROGRESS",
                Date.from(occurrence.startedAt()));
    }
}
