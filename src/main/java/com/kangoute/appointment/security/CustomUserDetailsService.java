package com.kangoute.appointment.security;

import com.kangoute.appointment.config.DemoProperties;
import com.kangoute.appointment.enums.DemoAccountType;
import com.kangoute.appointment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DemoProperties demoProperties;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .filter(user -> demoProperties.isEnabled() || user.getDemoAccountType() == DemoAccountType.NONE)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable ou desactive"));
    }
}
