package com.kangoute.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {
    private boolean enabled;
    private Account user = new Account("demo.user@appointment.local");
    private Account admin = new Account("demo.admin@appointment.local");

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Account getUser() { return user; }
    public void setUser(Account user) { this.user = user; }
    public Account getAdmin() { return admin; }
    public void setAdmin(Account admin) { this.admin = admin; }

    public static class Account {
        private String email;
        private String password;
        private String firstName;
        private String lastName;

        public Account() { }
        public Account(String email) { this.email = email; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
