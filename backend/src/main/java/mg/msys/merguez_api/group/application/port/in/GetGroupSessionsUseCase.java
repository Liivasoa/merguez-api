package mg.msys.merguez_api.group.application.port.in;

import java.util.List;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;

public interface GetGroupSessionsUseCase {
    List<Session> execute(GroupId groupId);
}
