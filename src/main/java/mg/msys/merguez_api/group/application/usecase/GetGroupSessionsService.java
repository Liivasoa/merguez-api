package mg.msys.merguez_api.group.application.usecase;

import java.util.List;
import java.util.Objects;

import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.port.in.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;

public final class GetGroupSessionsService implements GetGroupSessionsUseCase {

    private final GroupRepository groupRepository;

    public GetGroupSessionsService(GroupRepository groupRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository);
    }

    @Override
    public List<Session> execute(GroupId groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId)).sessions();
    }
}
