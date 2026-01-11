package com.example.JobFinder.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

@Value
@Builder
public class ExperienceEntry {
    String title;
    String company;
    String startLabel;
    String endLabel;
    String description;

    public boolean hasCompany() {
        return StringUtils.hasText(company);
    }

    public boolean hasDescription() {
        return StringUtils.hasText(description);
    }

    public String getRangeLabel() {
        boolean hasStart = StringUtils.hasText(startLabel);
        boolean hasEnd = StringUtils.hasText(endLabel);

        if (!hasStart && !hasEnd) {
            return "";
        }
        if (hasStart && hasEnd) {
            return startLabel + " – " + endLabel;
        }
        return hasStart ? ("Từ " + startLabel) : ("Đến " + endLabel);
    }
}
