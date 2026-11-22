package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;

public interface CloseSessionUseCase {
    SessionClosureResult execute(CloseSessionCommand command);
}
