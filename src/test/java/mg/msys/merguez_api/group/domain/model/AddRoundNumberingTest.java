package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class AddRoundNumberingTest {

    @Test
    @DisplayName("A group member adds rounds with successive numbers to the same occurrence")
    void groupMemberAddsTwoRoundsWithSuccessiveNumbersToSameOccurrence() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        UserId fourthMember = new UserId(UUID.randomUUID());
        Group group = Group.create(
                "Tuesday dojo", admin, List.of(firstMember, secondMember, thirdMember, fourthMember));
        Session session = group.createSession(
                firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(firstMember, session);

        Round firstRound = group.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));
        Round secondRound = group.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(thirdMember, fourthMember), Optional.empty())));

        assertEquals(occurrence, firstRound.occurrence());
        assertEquals(occurrence, secondRound.occurrence());
        assertEquals(1, firstRound.number());
        assertEquals(2, secondRound.number());
    }
}
