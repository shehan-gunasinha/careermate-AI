package com.careermate.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Cross-industry skill dictionary used for skill extraction, normalisation and matching. */
public final class SkillCatalog {
    public static final List<String> SKILLS = List.of(
            // IT
            "Java", "Spring Boot", "Python", "JavaScript", "TypeScript", "React", "Angular", "Vue", "Node.js", "SQL",
            "PostgreSQL", "MySQL", "MongoDB", "REST APIs", "GraphQL", "Docker", "Kubernetes", "AWS", "Azure", "GCP",
            "Git", "CI/CD", "Linux", "C#", ".NET", "C++", "Go", "PHP", "HTML", "CSS", "Tailwind CSS", "Machine Learning",
            "Data Analysis", "Power BI", "Tableau", "Agile", "Scrum", "Microservices", "Testing", "Kafka", "Redis",
            "Cybersecurity", "DevOps", "Figma", "UI/UX Design",
            // Engineering
            "AutoCAD", "SolidWorks", "CAD", "Mechanical Design", "Thermodynamics", "Manufacturing", "Quality Assurance",
            "Project Management", "Maintenance", "Lean Six Sigma", "MATLAB", "PLC", "HVAC", "Electrical Design",
            // Finance & accounting
            "Accounting", "Bookkeeping", "Financial Reporting", "Auditing", "Taxation", "Budgeting", "Financial Analysis",
            "Excel", "QuickBooks", "SAP", "Accounts Payable", "Accounts Receivable", "IFRS", "Payroll",
            // Marketing
            "Digital Marketing", "SEO", "Content Marketing", "Social Media", "Google Analytics", "Email Marketing",
            "Copywriting", "Brand Management", "Market Research", "Google Ads", "Canva",
            // Business / admin / HR
            "Business Analysis", "Requirements Gathering", "Stakeholder Management", "Process Improvement", "Data Entry",
            "Microsoft Office", "Recruitment", "Employee Relations", "Onboarding", "HR Administration", "Report Writing",
            "Scheduling", "Negotiation", "Sales", "CRM", "Salesforce",
            // Customer service & hospitality
            "Customer Support", "Customer Service", "Communication", "Problem Solving", "Teamwork", "Leadership",
            "Time Management", "Ticketing Systems", "Conflict Resolution", "Food Safety", "Front Desk", "Event Planning",
            // Healthcare & education
            "Patient Care", "Clinical Documentation", "First Aid", "Medical Terminology", "Teaching", "Curriculum Development",
            "Lesson Planning", "Classroom Management", "Tutoring",
            // Design
            "Adobe Photoshop", "Adobe Illustrator", "Adobe InDesign", "Graphic Design", "Typography", "Branding", "Video Editing");

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("js", "javascript"), Map.entry("ts", "typescript"), Map.entry("k8s", "kubernetes"),
            Map.entry("postgres", "postgresql"), Map.entry("reactjs", "react"), Map.entry("react.js", "react"),
            Map.entry("node", "node.js"), Map.entry("nodejs", "node.js"), Map.entry("springboot", "spring boot"),
            Map.entry("spring-boot", "spring boot"), Map.entry("spring", "spring boot"), Map.entry("rest", "rest apis"),
            Map.entry("rest api", "rest apis"), Map.entry("restful apis", "rest apis"), Map.entry("restful api", "rest apis"),
            Map.entry("ms excel", "excel"), Map.entry("microsoft excel", "excel"), Map.entry("ml", "machine learning"),
            Map.entry("golang", "go"), Map.entry("amazon web services", "aws"), Map.entry("google cloud", "gcp"),
            Map.entry("photoshop", "adobe photoshop"), Map.entry("illustrator", "adobe illustrator"),
            Map.entry("ms office", "microsoft office"), Map.entry("customer care", "customer support"),
            Map.entry("ux design", "ui/ux design"), Map.entry("ui design", "ui/ux design"));

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    private SkillCatalog() {
    }

    public static String normalize(String skill) {
        if (skill == null) {
            return "";
        }
        String s = skill.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return SYNONYMS.getOrDefault(s, s);
    }

    /** Word-boundary aware search of a (lower-case) term in (lower-case) text. */
    public static boolean containsTerm(String textLower, String termLower) {
        if (textLower == null || termLower == null || termLower.isBlank()) {
            return false;
        }
        Pattern p = PATTERNS.computeIfAbsent(termLower,
                t -> Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(t) + "(?![a-z0-9+#])"));
        return p.matcher(textLower).find();
    }

    /** Returns catalogue skills mentioned in the text (canonical casing). */
    public static List<String> findIn(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String skill : SKILLS) {
            if (containsTerm(lower, skill.toLowerCase(Locale.ROOT))) {
                found.add(skill);
            }
        }
        for (Map.Entry<String, String> e : SYNONYMS.entrySet()) {
            if (e.getKey().length() > 3 && containsTerm(lower, e.getKey())) {
                String canonical = SKILLS.stream().filter(s -> s.equalsIgnoreCase(e.getValue())).findFirst().orElse(null);
                if (canonical != null && !found.contains(canonical)) {
                    found.add(canonical);
                }
            }
        }
        return found;
    }
}
