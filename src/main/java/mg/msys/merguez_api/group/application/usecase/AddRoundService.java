package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.port.in.AddRoundUseCase;
import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.exception.DomainConflictException;
import mg.msys.merguez_api.group.domain.exception.DomainValidationException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class AddRoundService implements AddRoundUseCase {

    private final LoadGroupWithActiveOccurrencePort groupLoader;
    private final RoundRepository roundRepository;

    public AddRoundService(LoadGroupWithActiveOccurrencePort groupLoader, RoundRepository roundRepository) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
        this.roundRepository = Objects.requireNonNull(roundRepository);
    }

    @Override
    public Round execute(AddRoundCommand command) {
        Group group = groupLoader.loadByGroupId(command.groupId())
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT));
        SessionOccurrence occurrence = group.activeOccurrences().stream().findFirst()
                .orElseThrow(() -> new RoundOperationRefusedException(
                        RoundOperationRefusedException.Reason.CONFLICT));
        Round round;
        try {
            round = group.addRound(command.requester(), occurrence, command.combats());
        } catch (DomainConflictException exception) {
            throw new RoundOperationRefusedException(
                    RoundOperationRefusedException.Reason.CONFLICT, exception);
        } catch (DomainValidationException exception) {
            throw new RoundOperationRefusedException(
                    RoundOperationRefusedException.Reason.INVALID_REQUEST, exception);
        }
        roundRepository.create(round);
        return round;
    }
}
