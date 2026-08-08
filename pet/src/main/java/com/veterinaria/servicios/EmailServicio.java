package com.veterinaria.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServicio {

    private final JavaMailSender mailSender;
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoConfirmacion(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@veterinaria.com");
            message.setTo(toEmail);
            message.setSubject("Activa tu cuenta de Cliente - VetCare");

            String urlConfirmacion = frontendUrl + "/confirmar?token=" + token;

            message.setText("Hola,\n\n"
                    + "Tu registro en nuestra clínica ha comenzado. "
                    + "Por favor, haz clic en el siguiente enlace para crear tu contraseña y activar tu cuenta:\n\n"
                    + urlConfirmacion + "\n\n"
                    + "Si no solicitaste este correo, puedes ignorarlo.");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando el correo SMTP: " + e.getMessage());
        }
    }

    public void enviarCorreoRegistroCliente(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@veterinaria.com");
            message.setTo(toEmail);
            message.setSubject("Completa tu registro - VetCare");

            String urlConfirmacion = frontendUrl + "/completar-registro?token=" + token
                    + "&email=" + URLEncoder.encode(toEmail, StandardCharsets.UTF_8);

            message.setText("Hola,\n\n"
                    + "Para terminar de configurar tu cuenta, haz clic en el siguiente enlace y completa tus datos:\n\n"
                    + urlConfirmacion + "\n\n"
                    + "Si no solicitaste este correo, puedes ignorarlo.");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando el correo SMTP: " + e.getMessage());
        }
    }

    public void enviarCorreoResetPassword(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@veterinaria.com");
            message.setTo(toEmail);
            message.setSubject("Recuperación de Contraseña - VetCare");

            String urlConfirmacion = frontendUrl + "/confirmar?token=" + token + "&action=reset";

            message.setText("Hola,\n\n"
                    + "Hemos recibido una solicitud para restablecer tu contraseña.\n"
                    + "Por favor, haz clic en el siguiente enlace para crear una nueva contraseña:\n\n"
                    + urlConfirmacion + "\n\n"
                    + "Si no solicitaste esto, puedes ignorar este correo y tu contraseña no cambiará.");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando el correo SMTP (Reset Password): " + e.getMessage());
        }
    }
}
