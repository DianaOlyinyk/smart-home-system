package server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaskingMessageConverterTest {

    @Test
    void masksCompleteJsonValueContainingEscapedQuote() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("{\"password\":\"left\\\"right\"}");

        String masked = new MaskingMessageConverter().convert(event);

        assertEquals("{\"password\":\"***\"}", masked);
    }
}