package com.careermate.assistant;

import com.careermate.matching.CandidateContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Rule-based answers used when Gemini is not configured or unavailable. Never invents facts about the user. */
@Component
public class FallbackAssistant {

    private static final Map<String, List<String>> INTERVIEW_BANKS = Map.of(
            "software", List.of("Walk me through a project you built end to end and the trade-offs you made.",
                    "How do you debug a production issue you cannot reproduce locally?",
                    "Explain the difference between a process and a thread, or REST and GraphQL.",
                    "How do you approach code reviews and testing?",
                    "Tell me about a time you learned a new technology quickly."),
            "accountant", List.of("Explain the difference between accrual and cash accounting.",
                    "How do you reconcile a bank statement with the ledger?",
                    "Describe how you ensure accuracy and meet month-end deadlines.",
                    "Which accounting software and reporting standards (e.g. IFRS) have you used?",
                    "Tell me about a time you found and fixed a discrepancy."),
            "customer", List.of("How would you handle an angry customer?",
                    "Describe a time you went above and beyond for a customer.",
                    "How do you prioritise multiple tickets or calls at once?",
                    "What would you do if you did not know the answer to a customer's question?"),
            "marketing", List.of("How do you measure the success of a campaign?",
                    "Describe a campaign you contributed to and your specific role.",
                    "How do you decide which channels to use for a given audience?",
                    "How do you stay up to date with marketing trends and tools?"),
            "engineer", List.of("Describe a technical problem you solved and the approach you used.",
                    "How do you ensure safety and quality in your designs?",
                    "Which tools (CAD, simulation, testing) have you used and for what?",
                    "Tell me about a time a design failed and what you learned."));

    public String reply(String message, CandidateContext ctx) {
        String m = message.toLowerCase(Locale.ROOT);
        String body;
        if (has(m, "interview", "prepare")) {
            body = interview(m);
        } else if (has(m, "salary", "pay expectation", "compensation")) {
            body = salary();
        } else if (has(m, "email", "cover letter", "application letter", "write a letter")) {
            body = coverEmail(ctx);
        } else if (has(m, "resume", "cv ")) {
            body = resume();
        } else if (has(m, "plan", "roadmap", "development plan")) {
            body = plan();
        } else if (has(m, "learn", "skill")) {
            body = skills(ctx);
        } else if (has(m, "suitable", "which job", "what job", "career path", "qualification", "suggest career")) {
            body = paths(ctx);
        } else if (has(m, "requirement", "job ad", "job description", "advert")) {
            body = jobAd();
        } else {
            body = general();
        }
        return body + "\n\n_CareerMate is currently running in offline mode, so this is a general guide rather than a personalised AI answer. "
                + "Add a GEMINI_API_KEY on the server to enable full AI responses._";
    }

    private String interview(String m) {
        String key = null;
        if (m.contains("software") || m.contains("developer") || m.contains("programmer")) key = "software";
        else if (m.contains("account") || m.contains("finance")) key = "accountant";
        else if (m.contains("customer") || m.contains("support")) key = "customer";
        else if (m.contains("marketing")) key = "marketing";
        else if (m.contains("engineer")) key = "engineer";
        String bank = key != null
                ? "Common questions for this field:\n" + INTERVIEW_BANKS.get(key).stream().map(q -> "- " + q + "\n").collect(Collectors.joining())
                : "Common questions:\n- Tell me about yourself.\n- Why do you want this role and our company?\n"
                + "- Describe a challenge you overcame.\n- What are your strengths and areas to improve?\n";
        return "**Interview preparation**\n\n" + bank + "\n**How to prepare**\n"
                + "1. Research the company, its products and the job description; list the 5 skills it stresses most.\n"
                + "2. Prepare 4-5 stories using the STAR method (Situation, Task, Action, Result) from your real experience.\n"
                + "3. Prepare 3 thoughtful questions to ask the interviewer.\n"
                + "4. Practise answering out loud and time yourself; confirm logistics the day before.";
    }

    private String salary() {
        return "**Answering salary expectation questions**\n\n"
                + "1. Research typical pay for the role, level and location (job boards, salary surveys, people in your network).\n"
                + "2. Give a range based on your research, anchored toward the upper-middle of what you would accept.\n"
                + "3. Example: \"Based on my research for this role and my experience, I'm looking for something in the range of [X to Y], "
                + "and I'm open to discussing the full package.\"\n"
                + "4. If asked too early, it is fine to say you would like to learn more about the responsibilities first.\n"
                + "5. Consider the whole package: benefits, leave, bonus, training and flexibility.";
    }

    private String coverEmail(CandidateContext ctx) {
        String name = ctx == null || ctx.fullName() == null ? "[Your Name]" : ctx.fullName();
        return "**Job application email template** (fill in the brackets with your real details)\n\n"
                + "Subject: Application for [Job Title] - " + name + "\n\n"
                + "Dear [Hiring Manager's Name],\n\n"
                + "I am writing to apply for the [Job Title] position at [Company]. [One sentence on your most relevant experience or qualification.]\n\n"
                + "In my [role/studies/projects], I [specific, truthful achievement or responsibility relevant to the job]. "
                + "I am particularly interested in [Company] because [specific reason].\n\n"
                + "I have attached my resume and would welcome the opportunity to discuss how I can contribute to your team.\n\n"
                + "Kind regards,\n" + name + "\n[Phone] | [Email]";
    }

    private String resume() {
        return "**Improving your resume**\n\n"
                + "- Put contact details and a 2-4 line professional summary at the top.\n"
                + "- Use standard headings (Experience, Education, Skills) so applicant tracking systems can read it.\n"
                + "- Start bullets with action verbs and add measurable results where they are true.\n"
                + "- Mirror important keywords from the job description, but only for skills you genuinely have.\n"
                + "- Keep it to 1-2 pages and proofread for spelling and consistent date formats.\n\n"
                + "Tip: upload your resume in **Resume Studio** to get a section-by-section score and recommendations.";
    }

    private String plan() {
        return "**Career development plan (template)**\n\n"
                + "1. *Where I am*: list your current skills, experience and strengths.\n"
                + "2. *Where I want to be*: define a target role and timeframe (e.g. 12 months).\n"
                + "3. *Gap analysis*: compare 3-5 job descriptions for the target role with your skills.\n"
                + "4. *30 days*: pick 1-2 skills to learn, update your resume and profile.\n"
                + "5. *60 days*: complete a small project or course and add it to your portfolio.\n"
                + "6. *90 days*: apply to targeted roles weekly, network, and review progress.";
    }

    private String skills(CandidateContext ctx) {
        String mine = ctx != null && !ctx.skills().isEmpty()
                ? "Your profile currently lists: " + String.join(", ", ctx.skills().stream().limit(10).toList()) + ".\n\n" : "";
        return "**Deciding which skills to learn**\n\n" + mine
                + "1. Open 5 job postings for the role you want and note the skills that appear in most of them.\n"
                + "2. Separate must-have skills from nice-to-have ones.\n"
                + "3. Check the match page of a job in CareerMate to see exactly which skills are missing for you.\n"
                + "4. Learn by building: a small project, a course certificate or volunteer work gives you something concrete to show.";
    }

    private String paths(CandidateContext ctx) {
        String base = ctx != null && ctx.hasData()
                ? "You can open **Recommended Jobs** to see roles ranked by how well they match your profile and resume."
                : "Complete your profile and upload your resume so **Recommended Jobs** can rank roles for you.";
        return "**Finding suitable roles and career paths**\n\n" + base + "\n\n"
                + "To explore paths: list the subjects and tasks you enjoy, match them to job families, read job descriptions in those families, "
                + "and talk to people working in them. Tell me your field of study and interests and I can suggest directions to research.";
    }

    private String jobAd() {
        return "**Reading a job advertisement**\n\n"
                + "- *Responsibilities*: what you will do daily.\n"
                + "- *Required* qualifications: usually must-haves; *preferred* ones are bonuses.\n"
                + "- You do not need to meet every item; if you meet around 60-70% of requirements, applying is reasonable.\n"
                + "Paste the job text here and I can help you interpret it once AI mode is enabled.";
    }

    private String general() {
        return "I'm CareerMate AI Assistant. I can help with:\n\n"
                + "- Suggesting suitable roles and career paths\n- Improving your resume\n- Choosing skills to learn\n"
                + "- Interview preparation and common questions\n- Salary expectation answers\n- Application emails and career plans\n\n"
                + "Try asking, for example: \"Give me common interview questions for an accountant\".";
    }

    private static boolean has(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
