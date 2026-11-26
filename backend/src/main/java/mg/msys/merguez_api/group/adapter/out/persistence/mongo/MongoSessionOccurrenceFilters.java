package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.Date;
import java.util.List;

import org.bson.Document;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

final class MongoSessionOccurrenceFilters {

    private MongoSessionOccurrenceFilters() {
    }

    static Document activeOccurrenceFilter(GroupId groupId) {
        return new Document("groupId", groupId.value().toString())
                .append("status", SessionOccurrenceDocument.IN_PROGRESS_STATUS);
    }

    static Document occurrenceFilter(SessionOccurrence occurrence) {
        Document filter = activeOccurrenceFilter(occurrence.groupId())
                .append("scheduleId", occurrence.scheduleId().toString())
                .append("startedAt", Date.from(occurrence.startedAt()));
        if (occurrence.startedAt().equals(Date.from(occurrence.startedAt()).toInstant())) {
            filter.append("$or", List.of(
                    new Document("startedAtExact", occurrence.startedAt().toString()),
                    new Document("startedAtExact", new Document("$exists", false))));
        } else {
            filter.append("startedAtExact", occurrence.startedAt().toString());
        }
        return filter;
    }
}
