package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.result.CurrentGroupSessionResult;
import mg.msys.merguez_api.group.domain.model.GroupId;

public interface GetCurrentGroupSessionUseCase {

    CurrentGroupSessionResult execute(GroupId groupId);
}
