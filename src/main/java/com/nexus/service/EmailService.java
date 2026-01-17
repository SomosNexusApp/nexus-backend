package com.nexus.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("🔐 Verifica tu cuenta en Nexus");

            // HTML PERSONALIZADO
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                    <h2 style="color: #4A90E2; text-align: center;">Bienvenido a Nexus</h2>
                    <p style="color: #333;">Hola,</p>
                    <p style="color: #555;">Gracias por registrarte. Para activar tu cuenta, por favor introduce el siguiente código de verificación en la aplicación:</p>
                    
                    <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 5px; margin: 20px 0;">
                        <span style="font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;">%s</span>
                    </div>
                    
                    <p style="color: #777; font-size: 12px; text-align: center;">Si no has solicitado este código, puedes ignorar este mensaje.</p>
                    <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #999; font-size: 10px; text-align: center;">&copy; 2026 Nexus App. Todos los derechos reservados.</p>
                </div>
                """.formatted(codigo);

            helper.setText(htmlContent, true); 

            mailSender.send(message);
            System.out.println("📧 Correo de verificación enviado a " + destinatario);

        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar correo: " + e.getMessage());
        }
    }
}