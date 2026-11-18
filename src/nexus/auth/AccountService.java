package nexus.auth;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

public class AccountService implements IAuthenticationProvider {
    private static final AccountService instance = new AccountService();
    private final List<UserProfile> profiles = new ArrayList<>();
    private UserProfile currentProfile;

    private AccountService() {
        // Default profiles (personal details scrubbed for production release)
        UserProfile ram = new UserProfile(
                "USER-01",
                "Ram Charan Vanga",
                "Lead Developer (Student Admin)",
                "NEXUS-ADMIN-2026 - CSE",
                "Campus Technology Park, Suite 402, Academic City",
                "NEXUS Capital Wallet",
                "👨‍💻",
                "Apex Master (Level 8)"
        );

        UserProfile prof = new UserProfile(
                "USER-02",
                "Dr. Academic Examiner",
                "Faculty Evaluator & Project Reviewer",
                "FAC-AOOP-2026 - Dept of Computational Intelligence",
                "Tech Park #508, Faculty Block, Main Campus",
                "SRM Faculty Concession Card",
                "👨‍🏫",
                "Executive Reviewer"
        );

        UserProfile peer = new UserProfile(
                "USER-03",
                "Campus Undergrad Peer",
                "Collaborator & Student Tester",
                "NEXUS-GUEST-2026 - Engineering",
                "Hostel Quad B, Student Housing",
                "UPI / Campus Pay",
                "🎓",
                "Novice Explorer (Level 2)"
        );

        profiles.add(ram);
        profiles.add(prof);
        profiles.add(peer);

        currentProfile = ram;
    }

    public static AccountService getInstance() {
        return instance;
    }

    public List<UserProfile> getProfiles() {
        return profiles;
    }

    public UserProfile getCurrentProfile() {
        return currentProfile;
    }

    public void switchProfile(UserProfile newProfile) {
        if (newProfile != null && !newProfile.getId().equals(currentProfile.getId())) {
            this.currentProfile = newProfile;
            EventBus.getInstance().publish(new NexusEvent("USER_SWITCHED", newProfile, "Auth"));
        }
    }

    // --- IAuthenticationProvider Implementation (Review 2 Rubric) ---
    @Override
    public UserProfile authenticate(String username, String credential) throws nexus.common.exceptions.AuthenticationFailedException {
        if (credential == null || credential.trim().isEmpty()) {
            throw new nexus.common.exceptions.AuthenticationFailedException(username, "Empty security credentials provided.");
        }
        
        // Find matching profile by id, name, or username alias
        UserProfile matched = null;
        for (UserProfile p : profiles) {
            if (p.getName().equalsIgnoreCase(username) || 
                p.getId().equalsIgnoreCase(username) ||
                (username.toLowerCase().contains("ram") && p.getId().equals("USER-01")) ||
                (username.toLowerCase().contains("faculty") && p.getId().equals("USER-02")) ||
                (username.toLowerCase().contains("peer") && p.getId().equals("USER-03")) ||
                (username.toLowerCase().contains("guest") && p.getId().equals("USER-03"))) {
                matched = p;
                break;
            }
        }
        
        if (matched == null) {
            throw new nexus.common.exceptions.AuthenticationFailedException(username, "Identity not recognized in user directory.");
        }

        // Verify password credential (accepts profile passwords or global master key)
        String cred = credential.trim();
        boolean valid = cred.equals("admin") || cred.equals("admin123") || cred.equals("nexus2026") ||
                        cred.equals("faculty") || cred.equals("guest") ||
                        (matched.getId().equals("USER-01") && cred.equals("admin")) ||
                        (matched.getId().equals("USER-02") && cred.equals("faculty")) ||
                        (matched.getId().equals("USER-03") && cred.equals("guest"));

        if (!valid) {
            throw new nexus.common.exceptions.AuthenticationFailedException(username, "Invalid password credentials. Access denied.");
        }

        this.currentProfile = matched;
        EventBus.getInstance().publish(new NexusEvent("USER_SWITCHED", matched, "Auth"));
        return matched;
    }

    @Override
    public UserProfile getActiveUser() {
        return getCurrentProfile();
    }

    @Override
    public void switchAccount(String userId) {
        for (UserProfile p : profiles) {
            if (p.getId().equalsIgnoreCase(userId)) {
                switchProfile(p);
                break;
            }
        }
    }

    @Override
    public List<UserProfile> getAllProfiles() {
        return getProfiles();
    }

    @Override
    public boolean hasPermission(String requiredRole) {
        if (currentProfile == null) return false;
        if (currentProfile.getRole().toLowerCase().contains("admin") || currentProfile.getRole().toLowerCase().contains("lead")) {
            return true;
        }
        return currentProfile.getRole().equalsIgnoreCase(requiredRole);
    }
}
