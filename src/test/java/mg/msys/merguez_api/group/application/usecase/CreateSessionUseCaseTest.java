package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.user.domain.model.UserId;

class CreateSessionUseCaseTest {

    @ParameterizedTest
    @MethodSource("validCreationRequests")
    void createsAndSavesSessionForGroupAdminOrMember(String requesterRole, String name) {
        UserId admin = userId();
        UserId member = userId();
        GroupId groupId = GroupId.random();
        Group group = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(member));
        UserId requester = "admin".equals(requesterRole) ? admin : member;
        InMemoryGroupRepository repository = new InMemoryGroupRepository(group);
        CreateSessionUseCase useCase = new CreateSessionUseCase(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, requester, name, DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Group expectedGroup = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(member));
        Session expectedSession =
                expectedGroup.createSession(requester, name, DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Session result = useCase.execute(command);

        assertEquals(expectedSession, result);
        assertEquals(1, repository.savedSessionLists.size());
        assertEquals(List.of(result), repository.savedSessionLists.getFirst());
        assertEquals(List.of(result), group.sessions());
    }

    @ParameterizedTest
    @MethodSource("missingSchedulingInformation")
    void rejectsMissingSchedulingInformationWithoutSavingGroup(DayOfWeek day, LocalTime startTime) {
        UserId admin = userId();
        GroupId groupId = GroupId.random();
        Group group = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(userId()));
        InMemoryGroupRepository repository = new InMemoryGroupRepository(group);
        CreateSessionUseCase useCase = new CreateSessionUseCase(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, admin, "Evening training", day, startTime);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertEquals(0, repository.savedSessionLists.size());
        assertEquals(List.of(), group.sessions());
    }

    private static Stream<Arguments> missingSchedulingInformation() {
        return Stream.of(
                Arguments.of(null, LocalTime.of(14, 0)),
                Arguments.of(DayOfWeek.TUESDAY, null));
    }

    private static Stream<Arguments> validCreationRequests() {
        return Stream.of("admin", "member")
                .flatMap(role -> Stream.of("Evening training", null, "")
                        .map(name -> Arguments.of(role, name)));
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static class InMemoryGroupRepository implements GroupRepository {

        private final Group group;
        private final List<List<Session>> savedSessionLists = new ArrayList<>();

        private InMemoryGroupRepository(Group group) {
            this.group = group;
        }

        @Override
        public void save(Group group) {
            savedSessionLists.add(group.sessions());
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            return group.id().equals(id) ? Optional.of(group) : Optional.empty();
        }
    }
}
