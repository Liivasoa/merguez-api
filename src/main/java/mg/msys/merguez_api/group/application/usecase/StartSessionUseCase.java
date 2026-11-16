package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
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
        SessionOccurrence occurrence = group.startSession(command.requester(), command.scheduledSession());
        occurrenceRepository.save(occurrence);
        return occurrence;
    }
}
