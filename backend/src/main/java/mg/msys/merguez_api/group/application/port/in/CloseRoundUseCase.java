package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.CloseRoundCommand;
import mg.msys.merguez_api.group.domain.model.Round;

public interface CloseRoundUseCase {
    Round execute(CloseRoundCommand command);
}
