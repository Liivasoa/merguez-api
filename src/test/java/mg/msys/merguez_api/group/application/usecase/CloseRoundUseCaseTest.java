package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.application.command.CloseRoundCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
import mg.msys.merguez_api.user.domain.model.UserId;

class CloseRoundUseCaseTest {

    @Test
    void savesClosedRoundAndRejectsMissingRoundWithoutSaving() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        var session = group.createSession(admin, "Practice", DayOfWeek.MONDAY, LocalTime.NOON);
        var occurrence = group.startSession(admin, session);
        Combat initial = new Combat(List.of(admin, member), Optional.empty());
        Round round = group.addRound(admin, occurrence, List.of(initial));
        Combat result = new Combat(initial.participants(), Optional.of(member));
        RoundRepository repository = mock(RoundRepository.class);
        CloseRoundUseCase useCase = new CloseRoundUseCase(id -> Optional.of(group), repository);

        assertThrows(RoundOperationRefusedException.class, () -> useCase.execute(
                new CloseRoundCommand(group.id(), admin, 2, List.of(result))));
        verify(repository, never()).replace(round, new Round(occurrence, 1, RoundStatus.CLOSED, List.of(result)));

        Round closed = useCase.execute(new CloseRoundCommand(group.id(), admin, 1, List.of(result)));
        assertEquals(RoundStatus.CLOSED, closed.status());
        assertEquals(List.of(result), closed.combats());
        verify(repository).replace(round, closed);
    }
}
