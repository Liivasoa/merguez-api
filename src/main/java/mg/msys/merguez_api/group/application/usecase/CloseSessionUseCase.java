package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class CloseSessionUseCase {
    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final SaveClosedSessionOccurrencePort occurrenceSaver;
    private final LogSessionClosurePort closureLogger;

    public CloseSessionUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            SaveClosedSessionOccurrencePort occurrenceSaver,
            LogSessionClosurePort closureLogger) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.occurrenceSaver = Objects.requireNonNull(occurrenceSaver);
        this.closureLogger = Objects.requireNonNull(closureLogger);
    }

    public SessionClosureResult execute(CloseSessionCommand command) {
        Group group = groupLoader.loadByGroupId(command.groupId())
                .orElseThrow(() -> new IllegalStateException("Group or active session occurrence not found"));
        SessionOccurrence occurrence = group.activeOccurrences().stream().findFirst().orElse(null);
        SessionClosureResult result = group.closeSession(command.requester(), occurrence);
        occurrenceSaver.save(result.occurrence());
        result.netScores().keySet().forEach(member -> closureLogger.log(member, result));
        return result;
    }
}
