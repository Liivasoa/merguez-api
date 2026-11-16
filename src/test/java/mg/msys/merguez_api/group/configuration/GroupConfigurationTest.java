package mg.msys.merguez_api.group.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;

class GroupConfigurationTest {

    @ParameterizedTest
    @ValueSource(classes = {
            CreateSessionUseCase.class,
            GetGroupSessionsUseCase.class,
            CreateGroupUseCase.class
    })
    void registersGroupUseCasesAsSpringBeans(Class<?> useCaseType) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(GroupRepository.class, () -> mock(GroupRepository.class));
            context.register(GroupConfiguration.class);
            context.refresh();

            assertNotNull(context.getBean(useCaseType));
        }
    }
}
