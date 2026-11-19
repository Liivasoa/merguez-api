package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class AddEmptyRoundTest {

    @Test
    @DisplayName("A group member cannot add an empty round")
    void groupMemberCannotAddEmptyRound() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(firstMember, secondMember));
        Session session = group.createSession(
                firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(firstMember, session);

        assertThrows(IllegalArgumentException.class, () -> group.addRound(firstMember, occurrence, List.of()));

        Round firstValidRound = group.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));

        assertEquals(1, firstValidRound.number());
    }
}
