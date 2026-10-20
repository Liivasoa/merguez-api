package mg.msys.merguez_api.domain.group;

public class CreateGroupUseCase {

    private final GroupRepository groupRepository;

    public CreateGroupUseCase(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public Group execute(CreateGroupCommand command) {
        Group group = Group.create(command.name(), command.description(), command.creatorId(), command.participantIds());
        groupRepository.save(group);
        return group;
    }
}
