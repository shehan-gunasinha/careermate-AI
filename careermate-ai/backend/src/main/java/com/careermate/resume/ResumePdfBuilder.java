package com.careermate.resume;

import com.careermate.resume.ResumeDtos.CertificationItem;
import com.careermate.resume.ResumeDtos.EducationItem;
import com.careermate.resume.ResumeDtos.ExperienceItem;
import com.careermate.resume.ResumeDtos.PersonalDto;
import com.careermate.resume.ResumeDtos.ProjectItem;
import com.careermate.resume.ResumeDtos.ResumeBuildRequest;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Renders the resume builder form into a clean, single-column, ATS-friendly PDF (templates: classic, modern). */
@Component
public class ResumePdfBuilder {

    public byte[] build(ResumeBuildRequest r) {
        boolean modern = "modern".equalsIgnoreCase(r.template());
        Color accent = modern ? new Color(0x1E, 0x2A, 0x5A) : Color.BLACK;
        Font name = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, accent);
        Font headline = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(0x47, 0x55, 0x69));
        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, modern ? new Color(0x0F, 0x76, 0x6E) : Color.BLACK);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.BLACK);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(0x47, 0x55, 0x69));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 44, 44);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            int align = modern ? Element.ALIGN_LEFT : Element.ALIGN_CENTER;
            PersonalDto p = r.personal();
            Paragraph title = new Paragraph(clean(p.fullName()), name);
            title.setAlignment(align);
            doc.add(title);
            if (notBlank(p.headline())) {
                Paragraph hl = new Paragraph(clean(p.headline()), headline);
                hl.setAlignment(align);
                doc.add(hl);
            }
            List<String> contact = new ArrayList<>();
            for (String c : new String[]{p.email(), p.phone(), p.location(), p.links()}) {
                if (notBlank(c)) contact.add(clean(c));
            }
            if (!contact.isEmpty()) {
                Paragraph cp = new Paragraph(String.join("  |  ", contact), small);
                cp.setAlignment(align);
                cp.setSpacingAfter(6);
                doc.add(cp);
            }

            if (notBlank(r.summary())) {
                section(doc, "PROFESSIONAL SUMMARY", h, accent);
                doc.add(new Paragraph(clean(r.summary()), body));
            }
            if (r.experience() != null && !r.experience().isEmpty()) {
                section(doc, "WORK EXPERIENCE", h, accent);
                for (ExperienceItem e : r.experience()) {
                    String dates = join(" - ", e.startDate(), e.current() ? "Present" : e.endDate());
                    Paragraph t = new Paragraph(clean(e.title()) + ", " + clean(e.company()) + (dates.isEmpty() ? "" : "  (" + dates + ")"), bold);
                    t.setSpacingBefore(4);
                    doc.add(t);
                    bullets(doc, e.description(), body);
                }
            }
            if (r.education() != null && !r.education().isEmpty()) {
                section(doc, "EDUCATION", h, accent);
                for (EducationItem e : r.education()) {
                    String deg = join(", ", e.degree(), e.fieldOfStudy());
                    Paragraph t = new Paragraph((deg.isEmpty() ? "" : clean(deg) + " - ") + clean(e.institution())
                            + (notBlank(e.period()) ? "  (" + clean(e.period()) + ")" : ""), bold);
                    t.setSpacingBefore(4);
                    doc.add(t);
                    bullets(doc, e.description(), body);
                }
            }
            if (r.skills() != null && !r.skills().isEmpty()) {
                section(doc, "SKILLS", h, accent);
                doc.add(new Paragraph(clean(String.join(", ", r.skills())), body));
            }
            if (r.projects() != null && !r.projects().isEmpty()) {
                section(doc, "PROJECTS", h, accent);
                for (ProjectItem pr : r.projects()) {
                    Paragraph t = new Paragraph(clean(pr.name()) + (notBlank(pr.url()) ? "  (" + clean(pr.url()) + ")" : ""), bold);
                    t.setSpacingBefore(4);
                    doc.add(t);
                    bullets(doc, pr.description(), body);
                }
            }
            if (r.certifications() != null && !r.certifications().isEmpty()) {
                section(doc, "CERTIFICATIONS", h, accent);
                for (CertificationItem c : r.certifications()) {
                    doc.add(new Paragraph("\u2022 " + clean(join(", ", c.name(), c.issuer(), c.year())), body));
                }
            }
            if (r.achievements() != null && !r.achievements().isEmpty()) {
                section(doc, "ACHIEVEMENTS", h, accent);
                for (String a : r.achievements()) {
                    if (notBlank(a)) doc.add(new Paragraph("\u2022 " + clean(a), body));
                }
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate PDF", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    /** Plain-text version of the resume, stored for analysis and AI context. */
    public String toPlainText(ResumeBuildRequest r) {
        StringBuilder sb = new StringBuilder();
        PersonalDto p = r.personal();
        sb.append(p.fullName()).append('\n');
        if (notBlank(p.headline())) sb.append(p.headline()).append('\n');
        sb.append(join(" | ", p.email(), p.phone(), p.location(), p.links())).append("\n\n");
        if (notBlank(r.summary())) sb.append("Professional Summary\n").append(r.summary()).append("\n\n");
        if (r.experience() != null && !r.experience().isEmpty()) {
            sb.append("Work Experience\n");
            for (ExperienceItem e : r.experience()) {
                sb.append(e.title()).append(", ").append(e.company()).append(" ")
                        .append(join(" - ", e.startDate(), e.current() ? "Present" : e.endDate())).append('\n');
                if (notBlank(e.description())) sb.append(e.description()).append('\n');
            }
            sb.append('\n');
        }
        if (r.education() != null && !r.education().isEmpty()) {
            sb.append("Education\n");
            for (EducationItem e : r.education()) {
                sb.append(join(", ", e.degree(), e.fieldOfStudy(), e.institution(), e.period())).append('\n');
                if (notBlank(e.description())) sb.append(e.description()).append('\n');
            }
            sb.append('\n');
        }
        if (r.skills() != null && !r.skills().isEmpty()) sb.append("Skills\n").append(String.join(", ", r.skills())).append("\n\n");
        if (r.projects() != null && !r.projects().isEmpty()) {
            sb.append("Projects\n");
            for (ProjectItem pr : r.projects()) {
                sb.append(pr.name()).append('\n');
                if (notBlank(pr.description())) sb.append(pr.description()).append('\n');
            }
            sb.append('\n');
        }
        if (r.certifications() != null && !r.certifications().isEmpty()) {
            sb.append("Certifications\n");
            for (CertificationItem c : r.certifications()) sb.append(join(", ", c.name(), c.issuer(), c.year())).append('\n');
            sb.append('\n');
        }
        if (r.achievements() != null && !r.achievements().isEmpty()) {
            sb.append("Achievements\n");
            r.achievements().stream().filter(ResumePdfBuilder::notBlank).forEach(a -> sb.append(a).append('\n'));
        }
        return sb.toString().trim();
    }

    private void section(Document doc, String title, Font h, Color accent) throws DocumentException {
        Paragraph p = new Paragraph(title, h);
        p.setSpacingBefore(10);
        doc.add(p);
        LineSeparator line = new LineSeparator(0.8f, 100, accent, Element.ALIGN_CENTER, -2);
        doc.add(line);
    }

    private void bullets(Document doc, String text, Font body) throws DocumentException {
        if (!notBlank(text)) {
            return;
        }
        for (String line : text.split("\\r?\\n")) {
            String l = line.trim().replaceFirst("^[\\u2022*\\-]\\s*", "");
            if (!l.isEmpty()) {
                Paragraph p = new Paragraph("\u2022 " + clean(l), body);
                p.setIndentationLeft(10);
                doc.add(p);
            }
        }
    }

    private static String join(String sep, String... parts) {
        List<String> out = new ArrayList<>();
        for (String s : parts) {
            if (notBlank(s)) out.add(s.trim());
        }
        return String.join(sep, out);
    }

    /** Standard PDF fonts only cover Latin-1/WinAnsi, so unsupported characters are replaced. */
    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\u2013' || c == '\u2014') sb.append('-');
            else if (c == '\u2018' || c == '\u2019') sb.append('\'');
            else if (c == '\u201c' || c == '\u201d') sb.append('"');
            else if (c < 256 || c == '\u2022') sb.append(c);
            else sb.append('?');
        }
        return sb.toString().trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
