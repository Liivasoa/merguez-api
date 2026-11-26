package mg.msys.merguez_api.group.application.port.in;

import mg.msys.merguez_api.group.application.command.AddCombatCommand;
import mg.msys.merguez_api.group.domain.model.Round;

public interface AddCombatUseCase {
    Round execute(AddCombatCommand command);
}
