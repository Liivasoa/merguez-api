package mg.msys.merguez_api.group.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;

@Configuration
public class GroupConfiguration {

    @Bean
    CreateGroupUseCase createGroupUseCase(GroupRepository groupRepository) {
        return new CreateGroupUseCase(groupRepository);
    }
}
