package edu.tcu.cs.projectpulse.auth;

import edu.tcu.cs.projectpulse.auth.dto.AuthResponse;
import edu.tcu.cs.projectpulse.auth.dto.LoginRequest;
import edu.tcu.cs.projectpulse.auth.dto.RegisterRequest;
import edu.tcu.cs.projectpulse.invitation.InvitationDisabledException;
import edu.tcu.cs.projectpulse.invitation.InvitationEntity;
import edu.tcu.cs.projectpulse.invitation.InvitationNotFoundException;
import edu.tcu.cs.projectpulse.invitation.InvitationRepository;
import edu.tcu.cs.projectpulse.invitation.InvitationStatus;
import edu.tcu.cs.projectpulse.user.UserEntity;
import edu.tcu.cs.projectpulse.user.UserRepository;
import edu.tcu.cs.projectpulse.user.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       InvitationRepository invitationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        InvitationEntity invitation = invitationRepository.findByToken(req.token())
                .orElseThrow(() -> new InvitationNotFoundException(req.token()));

        if (invitation.getStatus() == InvitationStatus.DISABLED) {
            throw new InvitationDisabledException();
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new InvitationAlreadyUsedException();
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException(req.email());
        }

        UserEntity user = new UserEntity();
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setEnabled(true);
        user.setInvitationToken(req.token());

        if (invitation.getRole() == UserRole.INSTRUCTOR) {
            if (req.accessCode() == null || !req.accessCode().equals(invitation.getAccessCode())) {
                throw new InvalidAccessCodeException();
            }
            user.setRole(UserRole.INSTRUCTOR);
            user.setMiddleInitial(req.middleInitial());
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitationRepository.save(invitation);
        } else {
            user.setRole(UserRole.STUDENT);
        }

        user = userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        UserEntity user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
