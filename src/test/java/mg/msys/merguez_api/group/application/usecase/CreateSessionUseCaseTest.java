package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.domain.exception.DuplicateSessionScheduleException;
import mg.msys.merguez_api.group.domain.exception.InvalidSessionScheduleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.user.domain.model.UserId;

class CreateSessionServiceTest {

    @Test
    void rejectsSessionOperationsForMissingGroupWithoutSaving() {
        GroupId groupId = GroupId.random();
        InMemoryGroupRepository repository = new InMemoryGroupRepository();
        CreateSessionService CreateSessionService = new CreateSessionService(repository);
        GetGroupSessionsService GetGroupSessionsService = new GetGroupSessionsService(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, userId(), "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertAll(
                () -> assertThrows(GroupNotFoundException.class, () -> CreateSessionService.execute(command)),
                () -> assertThrows(GroupNotFoundException.class, () -> GetGroupSessionsService.execute(groupId)),
                () -> assertEquals(0, repository.savedGroupIds.size()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"outsider", "otherGroupMember", "null"})
    void rejectsNonMembersWithoutMutatingOrSavingGroup(String requesterKind) {
        UserId admin = userId();
        UserId member = userId();
        UserId otherGroupMember = userId();
        Group otherGroup = Group.restore(GroupId.random(), "Other group", null, userId(), List.of(otherGroupMember));
        UserId requester = switch (requesterKind) {
            case "otherGroupMember" -> otherGroupMember;
            case "null" -> null;
            default -> userId();
        };
        GroupId groupId = GroupId.random();
        Group directGroup = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(member));
        Group storedGroup = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(member));
        Session directExisting = directGroup.createSession(
                admin, "Existing session", DayOfWeek.MONDAY, LocalTime.of(10, 0));
        Session storedExisting = storedGroup.createSession(
                admin, "Existing session", DayOfWeek.MONDAY, LocalTime.of(10, 0));
        InMemoryGroupRepository repository = new InMemoryGroupRepository(storedGroup, otherGroup);
        CreateSessionService useCase = new CreateSessionService(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, requester, "New session", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertAll(
                () -> assertThrows(GroupAccessDeniedException.class,
                        () -> directGroup.createSession(requester, "New session", DayOfWeek.TUESDAY, LocalTime.of(14, 0))),
                () -> assertEquals(List.of(directExisting), directGroup.sessions()),
                () -> assertThrows(GroupAccessDeniedException.class, () -> useCase.execute(command)),
                () -> assertEquals(List.of(storedExisting), storedGroup.sessions()),
                () -> assertEquals(0, repository.savedGroupIds.size()));
    }

    @ParameterizedTest
    @MethodSource("validCreationRequests")
    void createsAndSavesSessionForGroupAdminOrMember(String requesterRole, String name) {
        UserId admin = userId();
        UserId member = userId();
        GroupId groupId = GroupId.random();
        Group group = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(member));
        UserId requester = "admin".equals(requesterRole) ? admin : member;
        InMemoryGroupRepository repository = new InMemoryGroupRepository(group);
        CreateSessionService useCase = new CreateSessionService(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, requester, name, DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Session result = useCase.execute(command);

        assertAll(
                () -> assertEquals(groupId, result.groupId()),
                () -> assertNotNull(result.id()),
                () -> assertEquals(
                        name == null || name.isEmpty() ? "TUESDAY 14:00" : name,
                        result.name()),
                () -> assertEquals(DayOfWeek.TUESDAY, result.day()),
                () -> assertEquals(LocalTime.of(14, 0), result.startTime()),
                () -> assertEquals(1, repository.savedSessionLists.size()),
                () -> assertEquals(List.of(result), repository.savedSessionLists.getFirst()),
                () -> assertEquals(List.of(result), group.sessions()));
    }

    @ParameterizedTest
    @MethodSource("missingSchedulingInformation")
    void rejectsMissingSchedulingInformationWithoutSavingGroup(DayOfWeek day, LocalTime startTime) {
        UserId admin = userId();
        GroupId groupId = GroupId.random();
        Group group = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(userId()));
        InMemoryGroupRepository repository = new InMemoryGroupRepository(group);
        CreateSessionService useCase = new CreateSessionService(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, admin, "Evening training", day, startTime);

        assertThrows(InvalidSessionScheduleException.class, () -> useCase.execute(command));

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
        CreateSessionService useCase = new CreateSessionService(repository);
        CreateSessionCommand duplicateCommand = new CreateSessionCommand(
                firstGroup.id(), firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        assertThrows(DuplicateSessionScheduleException.class, () -> useCase.execute(duplicateCommand));

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

    @Test
    void includesCreatedSessionWhenRetrievingGroupSessions() {
        UserId admin = userId();
        GroupId groupId = GroupId.random();
        Group group = Group.restore(groupId, "Dojo du mardi", null, admin, List.of(userId()));
        InMemoryGroupRepository repository = new InMemoryGroupRepository(group);
        CreateSessionService CreateSessionService = new CreateSessionService(repository);
        CreateSessionCommand command =
                new CreateSessionCommand(groupId, admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        Session createdSession = CreateSessionService.execute(command);
        List<Session> sessions = new GetGroupSessionsService(repository).execute(groupId);

        assertEquals(List.of(createdSession), sessions);
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
