package com.aaliya.codeverdict.auth;

import com.aaliya.codeverdict.config.CodeVerdictProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the demo account (with the starter problems) on first start-up, if enabled. */
@Component
public class DemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountInitializer.class);

    private final CodeVerdictProperties props;
    private final UserRepository users;
    private final AuthService auth;

    public DemoAccountInitializer(CodeVerdictProperties props, UserRepository users, AuthService auth) {
        this.props = props;
        this.users = users;
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        CodeVerdictProperties.Demo demo = props.demo();
        if (!demo.enabled() || users.existsByUsernameIgnoreCase(demo.username())) {
            return;
        }
        auth.createUser(demo.username(), demo.username() + "@codeverdict.local", demo.password(), "Demo User", User.ROLE_DEMO);
        log.info("Created demo account '{}'", demo.username());
    }
}
