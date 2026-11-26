package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.domain.model.Round;

public interface AddRoundUseCase {
    Round execute(AddRoundCommand command);
}
