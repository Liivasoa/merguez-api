package mg.msys.merguez_api.group.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.application.usecase.AddCombatUseCase;
import mg.msys.merguez_api.group.application.usecase.AddRoundUseCase;
import mg.msys.merguez_api.group.application.usecase.CloseRoundUseCase;
import mg.msys.merguez_api.group.application.usecase.CloseSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.usecase.StartSessionUseCase;

@Configuration
public class GroupConfiguration {

    @Bean
    CloseSessionUseCase closeSessionUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            GroupRepository groupRepository,
            SaveClosedSessionOccurrencePort occurrenceSaver,
            LogSessionClosurePort closureLogger) {
        return new CloseSessionUseCase(groupLoader, groupRepository, occurrenceSaver, closureLogger);
    }

    @Bean
    CloseRoundUseCase closeRoundUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new CloseRoundUseCase(groupLoader, roundRepository);
    }

    @Bean
    AddRoundUseCase addRoundUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new AddRoundUseCase(groupLoader, roundRepository);
    }

    @Bean
    AddCombatUseCase addCombatUseCase(
            LoadGroupWithActiveOccurrencePort groupLoader,
            RoundRepository roundRepository) {
        return new AddCombatUseCase(groupLoader, roundRepository);
    }

    @Bean
    CreateGroupUseCase createGroupUseCase(GroupRepository groupRepository) {
        return new CreateGroupUseCase(groupRepository);
    }

    @Bean
    CreateSessionUseCase createSessionUseCase(GroupRepository groupRepository) {
        return new CreateSessionUseCase(groupRepository);
    }

    @Bean
    GetGroupSessionsUseCase getGroupSessionsUseCase(GroupRepository groupRepository) {
        return new GetGroupSessionsUseCase(groupRepository);
    }

    @Bean
    StartSessionUseCase startSessionUseCase(
            GroupRepository groupRepository,
            SessionOccurrenceRepository occurrenceRepository) {
        return new StartSessionUseCase(groupRepository, occurrenceRepository);
    }
}
