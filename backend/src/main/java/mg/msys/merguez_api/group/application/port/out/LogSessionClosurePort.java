package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.user.domain.model.UserId;

public interface LogSessionClosurePort {
    void log(UserId member, SessionClosureResult result);
}
