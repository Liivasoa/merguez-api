package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.domain.model.Session;

public interface CreateSessionUseCase {
    Session execute(CreateSessionCommand command);
}
