package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;
import java.util.Optional;

import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class CloseSessionUseCase {
    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final GroupRepository groupRepository;
    private final SaveClosedSessionOccurrencePort occurrenceSaver;
    private final LogSessionClosurePort closureLogger;

    public CloseSessionUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            GroupRepository groupRepository,
            SaveClosedSessionOccurrencePort occurrenceSaver,
            LogSessionClosurePort closureLogger) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.groupRepository = Objects.requireNonNull(groupRepository);
        this.occurrenceSaver = Objects.requireNonNull(occurrenceSaver);
        this.closureLogger = Objects.requireNonNull(closureLogger);
    }

    public SessionClosureResult execute(CloseSessionCommand command) {
        Optional<Group> activeGroup = groupLoader.loadByGroupId(command.groupId());
        Group group = activeGroup.orElseGet(() -> groupRepository.findById(command.groupId())
                .orElseThrow(() -> new GroupNotFoundException(command.groupId())));
        SessionOccurrence occurrence = activeGroup.isPresent()
                ? group.activeOccurrences().stream().findFirst().orElse(null)
                : null;
        if (occurrence != null && !occurrence.scheduleId().equals(command.scheduledSessionId())) {
            throw new IllegalStateException("Requested session is not the active session");
        }
        SessionClosureResult result = group.closeSession(command.requester(), occurrence);
        occurrenceSaver.save(result.occurrence());
        result.netScores().keySet().forEach(member -> closureLogger.log(member, result));
        return result;
    }
}
