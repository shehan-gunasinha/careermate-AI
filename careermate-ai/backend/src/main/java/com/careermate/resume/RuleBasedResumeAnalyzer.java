package com.careermate.resume;

import com.careermate.common.util.SkillCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Deterministic resume analysis used as the fallback when no AI key is configured or the AI call fails. */
@Component
public class RuleBasedResumeAnalyzer {
    public static final String SUMMARY = "Professional Summary";
    public static final String EDUCATION = "Education";
    public static final String EXPERIENCE = "Work Experience";
    public static final String SKILLS = "Skills";
    public static final String ACHIEVEMENTS = "Achievements";
    public static final String CERTS = "Certifications";
    public static final String KEYWORDS = "Keywords";
    public static final String FORMAT = "Formatting & Readability";
    public static final String ATS = "ATS Compatibility";

    private static final Map<String, Integer> WEIGHTS = new LinkedHashMap<>();

    static {
        WEIGHTS.put(SUMMARY, 10);
        WEIGHTS.put(EDUCATION, 12);
        WEIGHTS.put(EXPERIENCE, 25);
        WEIGHTS.put(SKILLS, 15);
        WEIGHTS.put(ACHIEVEMENTS, 8);
        WEIGHTS.put(CERTS, 5);
        WEIGHTS.put(KEYWORDS, 5);
        WEIGHTS.put(FORMAT, 10);
        WEIGHTS.put(ATS, 10);
    }

    private static final Pattern DATE_RANGE = Pattern.compile(
            "(19|20)\\d{2}\\s*(-|\\u2013|\\u2014|to)\\s*((19|20)\\d{2}|present|current|now)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BULLET = Pattern.compile("(?m)^\\s*[\\u2022\\u00b7*\\-\\u25aa\\u25cf]\\s+\\S");
    private static final Pattern QUANT = Pattern.compile("\\d+\\s?%|[$\\u00a3\\u20ac]\\s?\\d|\\b\\d{2,}\\+?\\s+(users|clients|customers|projects|members|employees|sales|students|patients|accounts|people|team)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED = Pattern.compile("\\b([A-Za-z]{2,})\\s+\\1\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> ACTION_VERBS = List.of("managed", "led", "developed", "built", "designed", "implemented",
            "improved", "created", "delivered", "launched", "reduced", "increased", "analysed", "analyzed", "coordinated",
            "organised", "organized", "trained", "negotiated", "maintained", "supported", "achieved", "optimised", "optimized");
    private static final Map<String, String> MISSPELLINGS = Map.ofEntries(
            Map.entry("recieve", "receive"), Map.entry("seperate", "separate"), Map.entry("definately", "definitely"),
            Map.entry("occured", "occurred"), Map.entry("managment", "management"), Map.entry("experiance", "experience"),
            Map.entry("responsibile", "responsible"), Map.entry("acheive", "achieve"), Map.entry("sucessful", "successful"),
            Map.entry("knowlege", "knowledge"), Map.entry("enviroment", "environment"), Map.entry("developement", "development"),
            Map.entry("comunication", "communication"), Map.entry("proffesional", "professional"), Map.entry("langauge", "language"));

    public AnalysisData analyze(String text, String extension) {
        String t = text == null ? "" : text;
        String lower = t.toLowerCase(Locale.ROOT);
        int words = t.isBlank() ? 0 : t.trim().split("\\s+").length;
        boolean hasEmail = TextExtractor.EMAIL.matcher(t).find();
        boolean hasPhone = TextExtractor.PHONE.matcher(t).find();
        Map<String, Boolean> has = new LinkedHashMap<>();
        TextExtractor.SECTION_HEADINGS.forEach((k, p) -> has.put(k, p.matcher(t).find()));
        int bullets = count(BULLET, t);
        int dateRanges = count(DATE_RANGE, t);
        int quant = count(QUANT, t);
        int verbs = (int) ACTION_VERBS.stream().filter(v -> SkillCatalog.containsTerm(lower, v)).count();
        List<String> skillsFound = SkillCatalog.findIn(t);
        int eduRank = com.careermate.matching.CandidateContextService.detectEducationRank(t);

        Map<String, Integer> s = new LinkedHashMap<>();
        s.put(SUMMARY, has.get("Summary") ? 85 : 25);
        s.put(EDUCATION, clamp((has.get("Education") ? 50 : 0) + (eduRank > 0 ? 30 : 0) + (Pattern.compile("(19|20)\\d{2}").matcher(t).find() ? 20 : 0)));
        s.put(EXPERIENCE, clamp((has.get("Experience") ? 40 : 0) + (bullets >= 5 ? 20 : bullets * 4) + (dateRanges >= 1 ? 20 : 0) + Math.min(20, verbs * 5)));
        s.put(SKILLS, clamp((has.get("Skills") ? 40 : 0) + Math.min(60, skillsFound.size() * 8)));
        s.put(ACHIEVEMENTS, clamp((has.get("Achievements") ? 40 : 0) + Math.min(60, quant * 20)));
        s.put(CERTS, has.get("Certifications") ? 85 : (lower.matches("(?s).*\\b(certified|certificate|certification)\\b.*") ? 60 : 20));
        s.put(KEYWORDS, clamp(skillsFound.size() * 9 + verbs * 2));

        int fmt = 100;
        if (words < 250) fmt -= 30;
        if (words > 1000) fmt -= 25;
        if (bullets == 0) fmt -= 15;
        if (avgSentenceLength(t) > 28) fmt -= 15;
        if (!hasEmail) fmt -= 20;
        if (!hasPhone) fmt -= 10;
        s.put(FORMAT, clamp(fmt));

        int ats = 100;
        if (!hasEmail) ats -= 20;
        if (!hasPhone) ats -= 10;
        for (String h : List.of("Experience", "Education", "Skills")) {
            if (!has.get(h)) ats -= 15;
        }
        if (words < 200) ats -= 20;
        if (t.chars().filter(c -> c == '|' || c == '\u2605' || c == '\u25ba' || c == '\u2756').count() > 10) ats -= 10;
        s.put(ATS, clamp(ats));

        double total = 0;
        int weightSum = 0;
        for (Map.Entry<String, Integer> w : WEIGHTS.entrySet()) {
            total += s.get(w.getKey()) * w.getValue();
            weightSum += w.getValue();
        }
        int overall = clamp((int) Math.round(total / weightSum));

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recs = new ArrayList<>();
        s.forEach((name, score) -> {
            if (score >= 80) strengths.add(name + " is well covered (" + score + "/100).");
            else if (score < 60) weaknesses.add(name + " needs attention (" + score + "/100).");
        });
        if (!skillsFound.isEmpty()) strengths.add("Recognised skills: " + String.join(", ", skillsFound.stream().limit(8).toList()) + ".");

        List<String> missing = new ArrayList<>();
        if (!hasEmail) missing.add("Email address");
        if (!hasPhone) missing.add("Phone number");
        if (!has.get("Summary")) missing.add("Professional summary or career objective");
        if (!has.get("Experience")) missing.add("Work experience section (internships, projects or volunteering count too)");
        if (!has.get("Education")) missing.add("Education section");
        if (!has.get("Skills")) missing.add("Skills section");
        if (dateRanges == 0) missing.add("Employment or study dates in a consistent format (e.g. 2022 - 2024)");
        if (!lower.contains("linkedin") && !lower.contains("github") && !lower.contains("portfolio") && !lower.contains("http")) {
            missing.add("Profile or portfolio link (LinkedIn, GitHub or portfolio, where relevant to your field)");
        }

        if (s.get(SUMMARY) < 60) recs.add("Add a 2-4 sentence professional summary describing your field, strengths and goals, using only facts you can support.");
        if (s.get(EXPERIENCE) < 70) recs.add("Describe each role with 3-5 bullet points starting with action verbs (managed, developed, improved) and include dates.");
        if (s.get(ACHIEVEMENTS) < 60) recs.add("Add measurable results where you truthfully can (percentages, volumes, savings, team sizes).");
        if (s.get(SKILLS) < 70) recs.add("List your core skills in a dedicated section, grouped by type, and mirror the keywords used in jobs you want.");
        if (s.get(CERTS) < 60) recs.add("List relevant certifications, licences or short courses, if you have any.");
        if (words < 250) recs.add("Your resume looks short; expand on responsibilities, projects and outcomes (aim for roughly 400-800 words).");
        if (words > 1000) recs.add("Your resume is long; trim it to the most relevant 1-2 pages.");
        if (s.get(ATS) < 80) recs.add("Use standard headings (Experience, Education, Skills), avoid tables and graphics, and keep contact details at the top for ATS compatibility.");
        if (recs.isEmpty()) recs.add("Tailor your summary and keywords to each job description before applying.");

        List<String> lang = languageIssues(t);

        String best = s.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        String worst = s.entrySet().stream().min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        String summary = "Your resume scores " + overall + "/100 in an automated review. Strongest area: " + best
                + ". Biggest opportunity: " + worst + ".";
        return new AnalysisData(overall, s, strengths, weaknesses, missing, recs, lang, summary);
    }

    private List<String> languageIssues(String t) {
        List<String> issues = new ArrayList<>();
        Matcher m = REPEATED.matcher(t);
        int repeated = 0;
        while (m.find() && repeated < 3) {
            issues.add("Repeated word: \"" + m.group(1) + " " + m.group(1) + "\".");
            repeated++;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        MISSPELLINGS.forEach((wrong, right) -> {
            if (SkillCatalog.containsTerm(lower, wrong) && issues.size() < 10) {
                issues.add("Possible misspelling: \"" + wrong + "\" (did you mean \"" + right + "\"?).");
            }
        });
        if (Pattern.compile("(?<![A-Za-z'])i(?![A-Za-z'.])").matcher(t).find() && Pattern.compile("\\bi\\s+(am|have|was|worked|led)\\b").matcher(t).find()) {
            issues.add("The pronoun \"I\" should be capitalised.");
        }
        if (count(Pattern.compile("\\.\\s+[a-z]"), t) > 3) {
            issues.add("Several sentences appear to start with a lower-case letter.");
        }
        return issues;
    }

    private static double avgSentenceLength(String t) {
        String[] sentences = t.split("[.!?]+\\s");
        if (sentences.length == 0) {
            return 0;
        }
        return (double) (t.isBlank() ? 0 : t.trim().split("\\s+").length) / sentences.length;
    }

    private static int count(Pattern p, String t) {
        Matcher m = p.matcher(t);
        int c = 0;
        while (m.find()) {
            c++;
        }
        return c;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
