package mg.msys.merguez_api.group.application.port.out;

import java.util.List;

import mg.msys.merguez_api.group.domain.model.GroupSummary;
import mg.msys.merguez_api.user.domain.model.UserId;

public interface LoadGroupSummariesPort {

    List<GroupSummary> loadGroupSummaries(UserId userId);
}
