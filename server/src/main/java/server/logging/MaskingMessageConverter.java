package server.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskingMessageConverter extends ClassicConverter {

    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(?i)(\\b(?:passwordHash|password|token|connectionToken)\\b\"?\\s*[:=]\\s*)(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^\\s,;}]+)"
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return null;
        }

        return maskSensitiveFields(message);
    }

    private String maskSensitiveFields(String message) {
        Matcher matcher = SENSITIVE_FIELD.matcher(message);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1);
            String value = matcher.group(2);
            if (value.startsWith("\"")) {
                replacement += "\"***\"";
            } else if (value.startsWith("'")) {
                replacement += "'***'";
            } else {
                replacement += "***";
            }
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(masked);
        return masked.toString();
    }
}