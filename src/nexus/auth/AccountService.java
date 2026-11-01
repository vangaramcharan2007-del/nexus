package nexus.auth;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

public class AccountService {
    private static final AccountService instance = new AccountService();
    private final List<UserProfile> profiles = new ArrayList<>();
    private UserProfile currentProfile;

    private AccountService() {
        // Default profiles
        UserProfile ram = new UserProfile(
                "USER-01",
                "Ram Charan Vanga",
                "Lead Developer (Student Admin)",
                "RA2511027010164 - SRM CSE (Big Data)",
                "Room 402, Kaveri Hostel, SRM University Kattankulathur, Chennai - 603203",
                "NEXUS Capital Wallet",
                "👨‍💻",
                "Apex Master (Level 8)"
        );

        UserProfile prof = new UserProfile(
                "USER-02",
                "Dr. Academic Examiner",
                "Faculty Evaluator & Project Reviewer",
                "FAC-AOOP-2026 - Dept of Computational Intelligence",
                "Tech Park #508, Faculty Block, SRMIST Main Campus, Chennai",
                "SRM Faculty Concession Card",
                "👨‍🏫",
                "Executive Reviewer"
        );

        UserProfile peer = new UserProfile(
                "USER-03",
                "SRM Undergrad Peer",
                "Collaborator & Student Tester",
                "RA2511027010999 - SRM Tech",
                "Adhiyaman Hostel Block-D, SRM Kattankulathur",
                "UPI / Google Pay",
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
}
