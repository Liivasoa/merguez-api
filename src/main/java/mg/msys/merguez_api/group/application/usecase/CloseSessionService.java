package mg.msys.merguez_api.group.application.usecase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import mg.msys.merguez_api.group.application.port.in.CloseSessionUseCase;
import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.exception.SessionClosureConflictException;
import mg.msys.merguez_api.group.application.exception.SessionClosureRefusedException;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.exception.SessionClosureRejectedException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class CloseSessionService implements CloseSessionUseCase {
    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final GroupRepository groupRepository;
    private final SaveClosedSessionOccurrencePort occurrenceSaver;
    private final LogSessionClosurePort closureLogger;

    public CloseSessionService(
            LoadGroupWithActiveOccurrencePort groupLoader,
            GroupRepository groupRepository,
            SaveClosedSessionOccurrencePort occurrenceSaver,
            LogSessionClosurePort closureLogger) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.groupRepository = Objects.requireNonNull(groupRepository);
        this.occurrenceSaver = Objects.requireNonNull(occurrenceSaver);
        this.closureLogger = Objects.requireNonNull(closureLogger);
    }

    @Override
    public SessionClosureResult execute(CloseSessionCommand command) {
        Optional<Group> activeGroup = groupLoader.loadByGroupId(command.groupId());
        Group group = activeGroup.orElseGet(() -> groupRepository.findById(command.groupId())
                .orElseThrow(() -> new GroupNotFoundException(command.groupId())));
        SessionOccurrence occurrence = activeGroup.isPresent()
                ? group.activeOccurrences().stream().findFirst().orElse(null)
                : null;
        if (occurrence != null && !occurrence.scheduleId().equals(command.scheduledSessionId())) {
            throw new SessionClosureRefusedException(
                    new IllegalStateException("Requested session is not the active session"));
        }
        List<Round> rounds = occurrence == null ? List.of() : List.copyOf(group.rounds(occurrence));
        SessionClosureResult result;
        try {
            result = group.closeSession(command.requester(), occurrence);
        } catch (SessionClosureRejectedException ex) {
            throw new SessionClosureRefusedException(ex);
        }
        try {
            occurrenceSaver.save(result.occurrence(), rounds);
        } catch (SessionClosureConflictException ex) {
            throw new SessionClosureRefusedException(ex);
        }
        result.netScores().keySet().forEach(member -> closureLogger.log(member, result));
        return result;
    }
}
