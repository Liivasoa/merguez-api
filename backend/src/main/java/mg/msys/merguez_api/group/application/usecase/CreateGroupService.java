package mg.msys.merguez_api.group.application.usecase;

import mg.msys.merguez_api.group.application.port.in.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.command.CreateGroupCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.model.Group;

public final class CreateGroupService implements CreateGroupUseCase {

    private final GroupRepository groupRepository;

    public CreateGroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public Group execute(CreateGroupCommand command) {
        Group group = Group.create(command.name(), command.description(), command.creatorId(), command.participantIds());
        groupRepository.save(group);
        return group;
    }
}
