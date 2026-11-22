package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.port.in.AddCombatUseCase;
import mg.msys.merguez_api.group.application.command.AddCombatCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class AddCombatService implements AddCombatUseCase {

    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final RoundRepository roundRepository;

    public AddCombatService(LoadGroupWithActiveOccurrencePort groupLoader, RoundRepository roundRepository) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.roundRepository = Objects.requireNonNull(roundRepository);
    }

    @Override
    public Round execute(AddCombatCommand command) {
        Group group = groupLoader.loadByGroupId(command.groupId())
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT,
                        new IllegalStateException("Group or active session occurrence not found")));
        SessionOccurrence occurrence = group.activeOccurrences().stream()
                .findFirst()
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT,
                        new IllegalStateException("Group or active session occurrence not found")));
        Round round = group.rounds(occurrence).stream()
                .filter(candidate -> candidate.number() == command.roundNumber())
                .findFirst()
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.INVALID_REQUEST,
                        new IllegalArgumentException("Round not found")));
        Round updatedRound;
        try {
            updatedRound = group.addCombat(command.requester(), round, command.combat());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new RoundOperationRefusedException(
                    exception instanceof IllegalStateException
                            ? RoundOperationRefusedException.Reason.CONFLICT
                            : RoundOperationRefusedException.Reason.INVALID_REQUEST,
                    exception);
        }
        roundRepository.replace(round, updatedRound);
        return updatedRound;
    }
}
