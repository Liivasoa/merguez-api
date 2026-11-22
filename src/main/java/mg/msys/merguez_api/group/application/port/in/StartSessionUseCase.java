package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public interface StartSessionUseCase {
    SessionOccurrence execute(StartSessionCommand command);
}
