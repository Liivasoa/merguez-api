package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.port.in.CloseRoundUseCase;
import mg.msys.merguez_api.group.application.command.CloseRoundCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.exception.DomainConflictException;
import mg.msys.merguez_api.group.domain.exception.DomainValidationException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class CloseRoundService implements CloseRoundUseCase {
    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final RoundRepository roundRepository;

    public CloseRoundService(LoadGroupWithActiveOccurrencePort groupLoader, RoundRepository roundRepository) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.roundRepository = Objects.requireNonNull(roundRepository);
    }

    @Override
    public Round execute(CloseRoundCommand command) {
        Group group = groupLoader.loadByGroupId(command.groupId())
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT,
                        "No active session is available for this group."));
        SessionOccurrence occurrence = group.activeOccurrences().stream().findFirst()
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT,
                        "No active session is available for this group."));
        Round round = group.rounds(occurrence).stream()
                .filter(candidate -> candidate.number() == command.roundNumber())
                .findFirst()
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.INVALID_REQUEST, "Round not found."));
        Round closed;
        try {
            closed = group.closeRound(command.requester(), round, command.combats());
        } catch (DomainConflictException exception) {
            throw new RoundOperationRefusedException(
                    RoundOperationRefusedException.Reason.CONFLICT, exception.getMessage(), exception);
        } catch (DomainValidationException exception) {
            throw new RoundOperationRefusedException(
                    RoundOperationRefusedException.Reason.INVALID_REQUEST, exception.getMessage(), exception);
        }
        roundRepository.replace(round, closed);
        return closed;
    }
}
