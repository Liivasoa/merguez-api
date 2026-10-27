package mg.msys.merguez_api.infrastructure.group;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mg.msys.merguez_api.domain.group.GroupRepository;

@Configuration
public class GroupInfrastructureConfiguration {

    @Bean
    GroupRepository groupRepository() {
        return new InMemoryGroupRepository();
    }
}
