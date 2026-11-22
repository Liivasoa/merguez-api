package mg.msys.merguez_api.group.application.usecase;

import java.util.Objects;

import mg.msys.merguez_api.group.application.port.in.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Session;

public final class CreateSessionService implements CreateSessionUseCase {

    private final GroupRepository groupRepository;

    public CreateSessionService(GroupRepository groupRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository);
    }

    @Override
    public Session execute(CreateSessionCommand command) {
        Group group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new GroupNotFoundException(command.groupId()));
        Session session = group.createSession(
                command.requester(),
                command.name(),
                command.day(),
                command.startTime());
        groupRepository.save(group);
        return session;
    }
}
