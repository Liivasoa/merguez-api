package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.CreateGroupCommand;
import mg.msys.merguez_api.group.domain.model.Group;

public interface CreateGroupUseCase {
    Group execute(CreateGroupCommand command);
}
