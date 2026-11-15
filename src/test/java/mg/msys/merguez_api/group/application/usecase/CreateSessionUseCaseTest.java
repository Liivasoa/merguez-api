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

import org.junit.jupiter.api.Test;
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

    @Test
    void rejectsDuplicateSessionWithinGroupButAllowsSameSlotInAnotherGroup() {
        UserId firstAdmin = userId();
        UserId firstMember = userId();
        Group firstGroup =
                Group.restore(GroupId.random(), "Dojo du mardi", null, firstAdmin, List.of(firstMember));
        Session existingSession = firstGroup.createSession(
                firstMember, "Morning training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        UserId secondAdmin = userId();
        UserId secondMember = userId();
        Group secondGroup =
                Group.restore(GroupId.random(), "Dojo du jeudi", null, secondAdmin, List.of(secondMember));
        InMemoryGroupRepository repository = new InMemoryGroupRepository(firstGroup, secondGroup);
        CreateSessionUseCase useCase = new CreateSessionUseCase(repository);
        CreateSessionCommand duplicateCommand = new CreateSessionCommand(
                firstGroup.id(), firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertThrows(IllegalStateException.class, () -> useCase.execute(duplicateCommand));

        assertEquals(List.of(existingSession), firstGroup.sessions());
        assertEquals(0, repository.savedSessionLists.size());

        CreateSessionCommand otherGroupCommand = new CreateSessionCommand(
                secondGroup.id(), secondMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        Session otherGroupSession = useCase.execute(otherGroupCommand);

        assertEquals(List.of(existingSession), firstGroup.sessions());
        assertEquals(List.of(otherGroupSession), secondGroup.sessions());
        assertEquals(List.of(secondGroup.id()), repository.savedGroupIds);
        assertEquals(List.of(List.of(otherGroupSession)), repository.savedSessionLists);
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

        private final List<Group> groups;
        private final List<GroupId> savedGroupIds = new ArrayList<>();
        private final List<List<Session>> savedSessionLists = new ArrayList<>();

        private InMemoryGroupRepository(Group... groups) {
            this.groups = List.of(groups);
        }

        @Override
        public void save(Group group) {
            savedGroupIds.add(group.id());
            savedSessionLists.add(group.sessions());
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            return groups.stream().filter(group -> group.id().equals(id)).findFirst();
        }
    }
}
