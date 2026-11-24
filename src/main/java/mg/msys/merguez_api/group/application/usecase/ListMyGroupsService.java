package mg.msys.merguez_api.group.application.usecase;

import java.util.List;
import java.util.Objects;

import mg.msys.merguez_api.group.application.port.in.ListMyGroupsUseCase;
import mg.msys.merguez_api.group.application.port.out.LoadGroupSummariesPort;
import mg.msys.merguez_api.group.domain.model.GroupSummary;
import mg.msys.merguez_api.user.domain.model.UserId;

public final class ListMyGroupsService implements ListMyGroupsUseCase {

    private final LoadGroupSummariesPort groupSummariesLoader;

    public ListMyGroupsService(LoadGroupSummariesPort groupSummariesLoader) {
        this.groupSummariesLoader = Objects.requireNonNull(groupSummariesLoader);
    }

    @Override
    public List<GroupSummary> execute(UserId userId) {
        return groupSummariesLoader.loadGroupSummaries(userId);
    }
}
