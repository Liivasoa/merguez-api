package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.exception.ScheduledSessionNotFoundException;
import mg.msys.merguez_api.group.application.exception.SessionAlreadyStartedException;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class StartSessionUseCase {

    private final GroupRepository groupRepository;
    private final SessionOccurrenceRepository occurrenceRepository;

    public StartSessionUseCase(
            GroupRepository groupRepository,
            SessionOccurrenceRepository occurrenceRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository);
        this.occurrenceRepository = Objects.requireNonNull(occurrenceRepository);
    }

    public SessionOccurrence execute(StartSessionCommand command) {
        Group group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new GroupNotFoundException(command.groupId()));
        var scheduledSession = group.sessions().stream()
                .filter(session -> session.id().equals(command.scheduledSessionId()))
                .findFirst()
                .orElseThrow(() -> new ScheduledSessionNotFoundException(command.scheduledSessionId()));
        if (occurrenceRepository.existsStartedByGroupId(group.id())) {
            throw new SessionAlreadyStartedException();
        }
        SessionOccurrence occurrence;
        try {
            occurrence = group.startSession(command.requester(), scheduledSession);
            occurrenceRepository.save(occurrence);
        } catch (IllegalStateException exception) {
            throw new SessionAlreadyStartedException(exception);
        }
        return occurrence;
    }
}
