package com.appointech.notifications.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpEmailSenderTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }

    @Test
    void sendsATextMessageWithFromRecipientSubjectAndBody() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        new SmtpEmailSender(provider(mailSender), "no-responder@appointech.com")
                .send("ana@mail.com", "Asunto", "Cuerpo");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("no-responder@appointech.com", message.getFrom());
        assertArrayEquals(new String[]{"ana@mail.com"}, message.getTo());
        assertEquals("Asunto", message.getSubject());
        assertEquals("Cuerpo", message.getText());
    }

    @Test
    void withoutConfiguredSenderTheFromFieldIsNotSet() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        new SmtpEmailSender(provider(mailSender), " ").send("ana@mail.com", "A", "C");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertNull(captor.getValue().getFrom());
    }

    @Test
    void withoutMailServerConfiguredItThrowsAClearException() {
        SmtpEmailSender sender = new SmtpEmailSender(provider(null), "x@y.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> sender.send("ana@mail.com", "A", "C"));

        assertTrue(ex.getMessage().contains("MAIL_HOST"));
    }

    @Test
    void aServerFailureReachesTheCaller() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("failed")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class,
                () -> new SmtpEmailSender(provider(mailSender), "x@y.com").send("ana@mail.com", "A", "C"));
    }
}
