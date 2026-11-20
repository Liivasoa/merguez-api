package mg.msys.merguez_api.group.adapter.out.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.user.domain.model.UserId;

@Component
public class SessionClosureLoggingAdapter implements LogSessionClosurePort {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionClosureLoggingAdapter.class);

    @Override
    public void log(UserId member, SessionClosureResult result) {
        LOGGER.info("Session closure for member {}: group {}, schedule {}, started at {}",
                member.value(), result.occurrence().groupId().value(), result.occurrence().scheduleId(),
                result.occurrence().startedAt());
    }
}
