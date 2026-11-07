package com.careermate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private Gemini gemini = new Gemini();
    private Seed seed = new Seed();
    private Jobs jobs = new Jobs();
    private Notifications notifications = new Notifications();
    private Mail mail = new Mail();
    private PasswordReset passwordReset = new PasswordReset();
    private Encryption encryption = new Encryption();

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 1440;
    }

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter @Setter
    public static class Storage {
        private String dir = "./data/resumes";
    }

    @Getter @Setter
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-3.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private int timeoutSeconds = 30;
    }

    @Getter @Setter
    public static class Seed {
        private boolean enabled = true;
        private String adminEmail = "admin@careermate.local";
        private String adminPassword = "Admin@12345";
        private String demoEmail = "demo@careermate.local";
        private String demoPassword = "Demo@12345";
    }

    @Getter @Setter
    public static class Jobs {
        private Sync sync = new Sync();
    }

    @Getter @Setter
    public static class Sync {
        private boolean enabled = false;
        private String cron = "0 0 */6 * * *";
        private boolean syncOnStartup = true;
        /** When true the recommendation feed prefers live external listings over seeded demo jobs. */
        private boolean preferExternal = true;
        private boolean remotiveEnabled = true;
        private boolean arbeitnowEnabled = true;
        private int maxPerSource = 50;
        private int staleAfterDays = 21;

        // ITPro.lk (https://itpro.lk/developer/) - keyless public job widget API, strong Sri Lankan coverage.
        private boolean itproEnabled = true;
        private int itproCount = 50;
        private int itproDaysBehind = 30;
        private String itproType = "0";
        private String itproCategory = "0";

        // Jooble (https://jooble.org/api/about) - requires a free API key.
        private boolean joobleEnabled = false;
        private String joobleApiKey = "";
        private String joobleKeywords = "software engineer";
        private String joobleLocation = "Sri Lanka";
        private int joobleMaxPages = 5;

        // JSearch on RapidAPI (https://rapidapi.com/letscrape-6bRBa3QguO5/api/jsearch) - requires a key.
        private boolean jsearchEnabled = false;
        private String jsearchApiKey = "";
        private String jsearchHost = "jsearch.p.rapidapi.com";
        private String jsearchQuery = "software engineer";
        private String jsearchCountry = "lk";
        private int jsearchNumPages = 1;
    }

    @Getter @Setter
    public static class Notifications {
        private String cron = "0 0 * * * *";
    }

    @Getter @Setter
    public static class Mail {
        private String host = "";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String from = "no-reply@careermate.local";
        private String fromName = "CareerMate AI";
        private boolean smtpAuth = true;
        private boolean starttls = true;
    }

    @Getter @Setter
    public static class PasswordReset {
        private int otpExpiryMinutes = 10;
        private int resendCooldownSeconds = 60;
        private int maxAttempts = 5;
    }

    @Getter @Setter
    public static class Encryption {
        /** Dedicated secret for encrypting user API keys at rest. Falls back to the JWT secret when blank. */
        private String secret = "";
    }
}
