package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.application.port.out.LoadGroupSummariesPort;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupSummary;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.user.domain.model.UserId;

class MongoGroupRepositoryUnitTest {

    @Test
    @DisplayName("No groups for a user produces an empty summary collection")
    void noGroupsForAUserProducesAnEmptySummaryCollection() {
        SpringDataGroupRepository springRepository = mock(SpringDataGroupRepository.class);
        MongoGroupRepository repository = new MongoGroupRepository(springRepository);
        LoadGroupSummariesPort groupSummariesLoader = repository;
        UserId requestedUser = new UserId(UUID.randomUUID());
        when(springRepository.findAllByMembersUserId(requestedUser.value().toString())).thenReturn(List.of());

        List<GroupSummary> summaries = groupSummariesLoader.loadGroupSummaries(requestedUser);

        assertEquals(List.of(), summaries);
        verify(springRepository).findAllByMembersUserId(requestedUser.value().toString());
    }

    @Test
    @DisplayName("Groups can be loaded by membership with their requested summaries")
    void groupsCanBeLoadedByMembershipWithTheirRequestedSummaries() {
        SpringDataGroupRepository springRepository = mock(SpringDataGroupRepository.class);
        MongoGroupRepository repository = new MongoGroupRepository(springRepository);
        LoadGroupSummariesPort groupSummariesLoader = repository;
        UserId requestedUser = new UserId(UUID.randomUUID());
        Group adminGroup = Group.create("Admin dojo", "Admin description", requestedUser,
                List.of(new UserId(UUID.randomUUID()), new UserId(UUID.randomUUID())));
        adminGroup.createSession(requestedUser, "Tuesday", DayOfWeek.TUESDAY, LocalTime.of(18, 0));
        adminGroup.createSession(requestedUser, "Thursday", DayOfWeek.THURSDAY, LocalTime.of(18, 0));
        Group memberGroup = Group.create("Member dojo", new UserId(UUID.randomUUID()), List.of(requestedUser));
        when(springRepository.findAllByMembersUserId(requestedUser.value().toString()))
                .thenReturn(List.of(GroupDocument.from(adminGroup), GroupDocument.from(memberGroup)));

        List<GroupSummary> summaries = groupSummariesLoader.loadGroupSummaries(requestedUser);

        assertEquals(2, summaries.size());
        assertTrue(summaries.contains(
                new GroupSummary("Admin dojo", Optional.of("Admin description"), 3, 2, true)));
        assertTrue(summaries.contains(new GroupSummary("Member dojo", Optional.empty(), 2, 0, false)));
        verify(springRepository).findAllByMembersUserId(requestedUser.value().toString());
    }

    @Test
    @DisplayName("A group loaded through the adapter includes its saved session")
    void savedGroupCanBeLoadedWithItsCreatedSession() {
        SpringDataGroupRepository springRepository = mock(SpringDataGroupRepository.class);
        Map<String, GroupDocument> documents = new HashMap<>();
        when(springRepository.save(any(GroupDocument.class))).thenAnswer(invocation -> {
            GroupDocument document = invocation.getArgument(0);
            documents.put(document.id(), document);
            return document;
        });
        when(springRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(documents.get(invocation.getArgument(0))));
        MongoGroupRepository repository = new MongoGroupRepository(springRepository);

        UserId creator = new UserId(UUID.randomUUID());
        UserId participant = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", creator, List.of(participant));
        Session session = group.createSession(creator, "Evening practice", DayOfWeek.TUESDAY, LocalTime.of(14, 0));

        repository.save(group);
        Group loaded = repository.findById(group.id()).orElseThrow();

        assertAll(
                () -> assertEquals(group.id(), loaded.id()),
                () -> assertTrue(loaded.sessions().contains(session)));
    }
}
