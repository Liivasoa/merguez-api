package mg.msys.merguez_api.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mg.msys.merguez_api.domain.group.CreateGroupUseCase;
import mg.msys.merguez_api.domain.group.GroupRepository;

@Configuration
public class UseCaseConfiguration {

    @Bean
    CreateGroupUseCase createGroupUseCase(GroupRepository groupRepository) {
        return new CreateGroupUseCase(groupRepository);
    }
}
