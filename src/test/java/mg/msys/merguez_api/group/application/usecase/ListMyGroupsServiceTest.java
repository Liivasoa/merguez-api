package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.application.port.in.ListMyGroupsUseCase;
import mg.msys.merguez_api.group.application.port.out.LoadGroupSummariesPort;
import mg.msys.merguez_api.group.domain.model.GroupSummary;
import mg.msys.merguez_api.user.domain.model.UserId;

class ListMyGroupsServiceTest {

    @Test
    void returnsGroupSummariesLoadedForTheRequestedUser() {
        UserId requestedUser = new UserId(UUID.randomUUID());
        List<GroupSummary> expectedSummaries = List.of(
                new GroupSummary("Tuesday dojo", Optional.of("Weekly sessions"), 4, 2, false),
                new GroupSummary("Thursday dojo", Optional.empty(), 3, 1, true));
        AtomicReference<UserId> loadedUser = new AtomicReference<>();
        LoadGroupSummariesPort port = userId -> {
            loadedUser.set(userId);
            return expectedSummaries;
        };
        ListMyGroupsUseCase useCase = new ListMyGroupsService(port);

        List<GroupSummary> result = useCase.execute(requestedUser);

        assertEquals(requestedUser, loadedUser.get());
        assertEquals(expectedSummaries, result);
    }

    @Test
    void returnsAnEmptyListWhenTheUserHasNoAssociatedGroups() {
        UserId requestedUser = new UserId(UUID.randomUUID());
        LoadGroupSummariesPort port = ignored -> List.of();
        ListMyGroupsUseCase useCase = new ListMyGroupsService(port);

        List<GroupSummary> result = useCase.execute(requestedUser);

        assertEquals(List.of(), result);
    }
}
