package com.kangoute.appointment;

import com.kangoute.appointment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-data-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "app.demo.enabled=false"
})
@Transactional
class DemoDataInitializerDisabledTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void demoDataIsNotCreatedWhenDisabled() {
        assertTrue(userRepository.findByEmail("demo@gestion-rendez-vous.com").isEmpty());
    }
}
