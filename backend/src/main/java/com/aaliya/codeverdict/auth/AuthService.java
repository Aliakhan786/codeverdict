package com.aaliya.codeverdict.auth;

import com.aaliya.codeverdict.auth.AuthDtos.MeResponse;
import com.aaliya.codeverdict.auth.AuthDtos.RegisterRequest;
import com.aaliya.codeverdict.auth.AuthDtos.StatsDto;
import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.config.CodeVerdictProperties;
import com.aaliya.codeverdict.gamification.Levels;
import com.aaliya.codeverdict.gamification.UserStats;
import com.aaliya.codeverdict.gamification.UserStatsRepository;
import com.aaliya.codeverdict.gamification.XpRules;
import com.aaliya.codeverdict.problem.ProblemSeeder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final UserStatsRepository stats;
    private final PasswordEncoder encoder;
    private final ProblemSeeder seeder;
    private final CodeVerdictProperties props;
    private final Clock clock;

    public AuthService(UserRepository users, UserStatsRepository stats, PasswordEncoder encoder,
                       ProblemSeeder seeder, CodeVerdictProperties props, Clock clock) {
        this.users = users;
        this.stats = stats;
        this.encoder = encoder;
        this.seeder = seeder;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (!props.auth().registrationEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REGISTRATION_DISABLED", "Sign-up is disabled on this server");
        }
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "That username is taken");
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "That email is already registered");
        }
        return createUser(req.username(), req.email(), req.password(),
                req.displayName() == null || req.displayName().isBlank() ? req.username() : req.displayName().trim(),
                User.ROLE_USER);
    }

    @Transactional
    public User createUser(String username, String email, String password, String displayName, String role) {
        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setDisplayName(displayName);
        u.setRole(role);
        u = users.save(u);
        UserStats s = new UserStats();
        s.setUserId(u.getId());
        stats.save(s);
        seeder.seedStarterProblems(u.getId());
        return u;
    }

    @Transactional(readOnly = true)
    public User login(String usernameOrEmail, String password) {
        String key = usernameOrEmail.trim();
        User u = (key.contains("@") ? users.findByEmailIgnoreCase(key) : users.findByUsernameIgnoreCase(key))
                .orElse(null);
        // Same error for unknown user and wrong password, so usernames cannot be probed.
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Wrong username or password");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Please log in again"));
        return toMe(u);
    }

    public MeResponse toMe(User u) {
        UserStats s = stats.findByUserId(u.getId()).orElseGet(UserStats::new);
        LocalDate today = LocalDate.now(clock);
        Levels.Level level = Levels.forXp(s.getTotalXp());
        Levels.Level next = Levels.next(level);
        StatsDto dto = new StatsDto(s.getTotalXp(), level.name(), level.index(),
                next == null ? null : next.name(),
                s.getTotalXp() - level.minXp(),
                next == null ? 0 : next.minXp() - level.minXp(),
                XpRules.visibleStreak(s.getLastActiveDate(), today, s.getCurrentStreak()),
                s.getLongestStreak());
        boolean readOnly = User.ROLE_DEMO.equals(u.getRole()) && props.demo().readOnly();
        return new MeResponse(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getTheme(),
                u.isSoundEnabled(), u.getDailyGoal(), readOnly, dto);
    }
}
