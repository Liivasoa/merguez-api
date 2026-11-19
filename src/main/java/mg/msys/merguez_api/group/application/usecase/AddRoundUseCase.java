package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class AddRoundUseCase {

    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final RoundRepository roundRepository;

    public AddRoundUseCase(LoadGroupWithActiveOccurrencePort groupLoader, RoundRepository roundRepository) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.roundRepository = Objects.requireNonNull(roundRepository);
    }

    public Round execute(AddRoundCommand command) {
        Group group = groupLoader.loadByGroupId(command.groupId())
                .orElseThrow(() -> new RoundOperationRefusedException(
                        new IllegalStateException("Group or active session occurrence not found")));
        SessionOccurrence occurrence = group.activeOccurrences().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Loaded group has no active session occurrence"));
        Round round;
        try {
            round = group.addRound(command.requester(), occurrence, command.combats());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new RoundOperationRefusedException(exception);
        }
        roundRepository.save(round);
        return round;
    }
}
