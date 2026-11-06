package com.careermate.resume;

import com.careermate.common.exception.BadRequestException;
import com.careermate.common.util.SkillCatalog;
import com.careermate.resume.ResumeDtos.ExtractedInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class TextExtractor {
    static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");

    static final Map<String, Pattern> SECTION_HEADINGS = new LinkedHashMap<>();

    static {
        int f = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
        SECTION_HEADINGS.put("Summary", Pattern.compile("^\\s*(professional\\s+|career\\s+)?(summary|profile|objective|about me)\\b", f));
        SECTION_HEADINGS.put("Education", Pattern.compile("^\\s*(education|academic)", f));
        SECTION_HEADINGS.put("Experience", Pattern.compile("^\\s*((work|professional|employment)\\s+)?(experience|history)|^\\s*employment", f));
        SECTION_HEADINGS.put("Skills", Pattern.compile("^\\s*(technical\\s+|key\\s+|core\\s+)?(skills|competencies)", f));
        SECTION_HEADINGS.put("Projects", Pattern.compile("^\\s*(projects|personal projects)", f));
        SECTION_HEADINGS.put("Achievements", Pattern.compile("^\\s*(achievements|accomplishments|awards|honou?rs)", f));
        SECTION_HEADINGS.put("Certifications", Pattern.compile("^\\s*(certifications?|licen[sc]es|courses|training)", f));
    }

    public String extract(byte[] bytes, String extension) {
        try {
            String text;
            if ("pdf".equals(extension)) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    text = new PDFTextStripper().getText(doc);
                }
            } else {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                     XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                    text = ex.getText();
                }
            }
            return text == null ? "" : text.replace("\u0000", "").replace("\r", "").trim();
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("The file could not be read. Make sure it is a valid, unprotected PDF or DOCX.");
        }
    }

    public ExtractedInfo info(String text) {
        String t = text == null ? "" : text;
        Matcher em = EMAIL.matcher(t);
        Matcher ph = PHONE.matcher(t);
        List<String> sections = new ArrayList<>();
        SECTION_HEADINGS.forEach((name, p) -> {
            if (p.matcher(t).find()) {
                sections.add(name);
            }
        });
        int words = t.isBlank() ? 0 : t.trim().split("\\s+").length;
        return new ExtractedInfo(em.find() ? em.group() : null, ph.find() ? ph.group().trim() : null, sections,
                SkillCatalog.findIn(t), words);
    }
}
