package com.kangoute.appointment;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import com.kangoute.appointment.repository.RoleRepository;
import com.kangoute.appointment.security.JwtService;
import com.kangoute.appointment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.Set;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class SecurityHardeningIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired JwtEncoder encoder;
    @Autowired JwtService jwtService;
    @Autowired UserService users;
    @Autowired RoleRepository roles;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void missingTokenReturns401() throws Exception {
        error(mvc.perform(get("/api/users/1")), 401, "/api/users/1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "a.b.c", "", "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyIn0."})
    void malformedTokenReturns401(String token) throws Exception {
        rejected(token);
    }

    @Test
    void invalidSignatureReturns401() throws Exception {
        User user = user();
        String token = jwtService.generateToken(user);
        int signature = token.lastIndexOf('.') + 1;
        token = token.substring(0, signature) + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
        rejected(token);
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        rejected(token(user().getEmail(), Instant.now().minusSeconds(2)));
    }

    @Test
    void missingExpirationReturns401() throws Exception {
        rejected(token(user().getEmail(), null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void invalidSubjectReturns401(String subject) throws Exception {
        rejected(token(subject, Instant.now().plusSeconds(120)));
    }

    @Test
    void unknownSubjectReturns401() throws Exception {
        rejected(token("unknown@example.com", Instant.now().plusSeconds(120)));
    }

    @Test
    void userCannotAccessAdminEndpoint() throws Exception {
        error(mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwtService.generateToken(user()))),
                403, "/api/admin/users");
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        User user = user();
        user.setRoles(Set.of(roles.findByName(RoleName.ROLE_ADMIN).orElseGet(() ->
                roles.save(com.kangoute.appointment.entity.Role.builder().name(RoleName.ROLE_ADMIN).build()))));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk());
    }

    @Test
    void registrationRemainsPublic() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Public","lastName":"User","email":"public@example.com","password":"secret123"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users/1/promote", "/api/users/1", "/api/appointments", "/api/admin/users"})
    void nestedAndOtherPostEndpointsRemainProtected(String path) throws Exception {
        error(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")), 401, path);
    }

    @Test
    void authenticationIsNotRetainedBetweenRequests() throws Exception {
        User user = user();
        mvc.perform(get("/api/users/" + user.getId()).header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
        error(mvc.perform(get("/api/users/" + user.getId())), 401, "/api/users/" + user.getId());
    }

    private User user() {
        User user = new User();
        user.setFirstName("Security");
        user.setLastName("User");
        user.setEmail("security@example.com");
        user.setPassword("secret123");
        return users.createUser(user);
    }

    private String token(String subject, Instant expiration) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuedAt(Instant.now().minusSeconds(300));
        if (subject != null) claims.subject(subject);
        if (expiration != null) claims.expiresAt(expiration);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    private void rejected(String token) throws Exception {
        error(mvc.perform(get("/api/users/1").header("Authorization", "Bearer " + token)), 401, "/api/users/1");
    }

    private void error(ResultActions result, int code, String path) throws Exception {
        result.andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.error").value(code == 401 ? "Unauthorized" : "Forbidden"))
                .andExpect(jsonPath("$.message").value(code == 401 ? "Authentification requise" : "Acces refuse"))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
