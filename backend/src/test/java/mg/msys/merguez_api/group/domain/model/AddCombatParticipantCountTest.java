package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.domain.exception.InvalidCombatException;
import mg.msys.merguez_api.user.domain.model.UserId;

class AddCombatParticipantCountTest {

    @Test
    @DisplayName("A combat cannot have one participant")
    void combatCannotHaveOneParticipant() {
        UserId participant = new UserId(UUID.randomUUID());

        assertThrows(
                InvalidCombatException.class,
                () -> new Combat(List.of(participant), Optional.empty()));
    }

    @Test
    @DisplayName("A combat cannot have three participants")
    void combatCannotHaveThreeParticipants() {
        UserId firstParticipant = new UserId(UUID.randomUUID());
        UserId secondParticipant = new UserId(UUID.randomUUID());
        UserId thirdParticipant = new UserId(UUID.randomUUID());

        assertThrows(
                InvalidCombatException.class,
                () -> new Combat(
                        List.of(firstParticipant, secondParticipant, thirdParticipant), Optional.empty()));
    }
}
