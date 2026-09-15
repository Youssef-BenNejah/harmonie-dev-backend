package com.harmoniedev.api.mail;

import com.harmoniedev.api.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {
	private static final String LOGO_CID = "harmonieLogo";
	private static final String LOGO_CLASSPATH = "static/mail/logo-mark.png";

	private final JavaMailSender mailSender;
	private final MailProperties mailProperties;
	private final AppProperties appProperties;

	public MailService(JavaMailSender mailSender, MailProperties mailProperties, AppProperties appProperties) {
		this.mailSender = mailSender;
		this.mailProperties = mailProperties;
		this.appProperties = appProperties;
	}

	public void sendWelcomeEmail(String toEmail, String firstName, String rawPassword) {
		String subject = "Bienvenue sur Harmonie-dev — vos identifiants";
		String html = welcomeTemplate(firstName, toEmail, rawPassword);
		send(toEmail, subject, html);
	}

	public void sendPasswordResetCode(String toEmail, String firstName, String code) {
		String subject = "Harmonie-dev — code de réinitialisation";
		String html = resetCodeTemplate(firstName, code);
		send(toEmail, subject, html);
	}

	public void sendPasswordChangedByAdminEmail(String toEmail, String firstName, String rawPassword) {
		String subject = "Harmonie-dev — votre mot de passe a été réinitialisé";
		String html = passwordChangedTemplate(firstName, toEmail, rawPassword);
		send(toEmail, subject, html);
	}

	/** Sends an invoice PDF as an attachment — the one email template built from real invoice/company data, not a fixed layout. */
	public void sendInvoiceEmail(String toEmail, String clientName, String invoiceLabel, byte[] pdfBytes, String fileName) {
		String subject = "Harmonie-dev — " + invoiceLabel;
		String greetingName = (clientName == null || clientName.isBlank()) ? "" : " " + escape(clientName);
		String html = baseLayout(
				"Votre facture",
				"""
				<p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
					Bonjour%s,
				</p>
				<p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155;">
					Veuillez trouver ci-joint votre document %s au format PDF.
				</p>
				"""
						.formatted(greetingName, escape(invoiceLabel)));
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
			helper.setText(html, true);
			helper.addInline(LOGO_CID, new ClassPathResource(LOGO_CLASSPATH));
			helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
			mailSender.send(message);
			log.info("Sent invoice email '{}' to {}", subject, toEmail);
		} catch (Exception ex) {
			log.error("Failed to send invoice email '{}' to {}", subject, toEmail, ex);
			throw new IllegalStateException("Failed to send invoice email", ex);
		}
	}

	private void send(String toEmail, String subject, String html) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
			helper.setText(html, true);
			helper.addInline(LOGO_CID, new ClassPathResource(LOGO_CLASSPATH));
			mailSender.send(message);
			log.info("Sent email '{}' to {}", subject, toEmail);
		} catch (Exception ex) {
			log.error("Failed to send email '{}' to {}", subject, toEmail, ex);
			throw new IllegalStateException("Failed to send email", ex);
		}
	}

	private String loginUrl() {
		return appProperties.getFrontendUrl() + "/login";
	}

	private String welcomeTemplate(String firstName, String email, String rawPassword) {
		String greetingName = (firstName == null || firstName.isBlank()) ? "" : " " + escape(firstName);
		return baseLayout(
				"Bienvenue sur Harmonie-dev",
				"""
				<p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
					Bonjour%s,
				</p>
				<p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155;">
					Votre compte Harmonie-dev a été créé. Voici vos identifiants de connexion —
					gardez-les en lieu sûr et changez votre mot de passe après votre première connexion.
				</p>
				<table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
					style="background:#EAF2FD;border:1px solid #CFE1F8;border-radius:14px;margin:0 0 28px;">
					<tr>
						<td style="padding:20px 24px;">
							<p style="margin:0 0 10px;font-size:12px;font-weight:700;letter-spacing:.04em;
								text-transform:uppercase;color:#1565C6;">Adresse e-mail</p>
							<p style="margin:0 0 18px;font-size:15px;font-family:'SFMono-Regular',Consolas,Menlo,monospace;
								color:#0B2E73;font-weight:600;">%s</p>
							<p style="margin:0 0 10px;font-size:12px;font-weight:700;letter-spacing:.04em;
								text-transform:uppercase;color:#1565C6;">Mot de passe temporaire</p>
							<p style="margin:0;font-size:15px;font-family:'SFMono-Regular',Consolas,Menlo,monospace;
								color:#0B2E73;font-weight:600;">%s</p>
						</td>
					</tr>
				</table>
				<div style="text-align:center;margin:0 0 8px;">
					<a href="%s" style="display:inline-block;background:#1565C6;color:#ffffff;text-decoration:none;
						font-size:15px;font-weight:600;padding:14px 32px;border-radius:12px;">
						Se connecter
					</a>
				</div>
				"""
						.formatted(greetingName, escape(email), escape(rawPassword), loginUrl()));
	}

	private String passwordChangedTemplate(String firstName, String email, String rawPassword) {
		String greetingName = (firstName == null || firstName.isBlank()) ? "" : " " + escape(firstName);
		return baseLayout(
				"Mot de passe réinitialisé",
				"""
				<p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
					Bonjour%s,
				</p>
				<p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155;">
					Un administrateur a réinitialisé le mot de passe de votre compte Harmonie-dev.
					Voici votre nouveau mot de passe temporaire — changez-le après votre prochaine connexion.
				</p>
				<table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
					style="background:#EAF2FD;border:1px solid #CFE1F8;border-radius:14px;margin:0 0 28px;">
					<tr>
						<td style="padding:20px 24px;">
							<p style="margin:0 0 10px;font-size:12px;font-weight:700;letter-spacing:.04em;
								text-transform:uppercase;color:#1565C6;">Adresse e-mail</p>
							<p style="margin:0 0 18px;font-size:15px;font-family:'SFMono-Regular',Consolas,Menlo,monospace;
								color:#0B2E73;font-weight:600;">%s</p>
							<p style="margin:0 0 10px;font-size:12px;font-weight:700;letter-spacing:.04em;
								text-transform:uppercase;color:#1565C6;">Nouveau mot de passe</p>
							<p style="margin:0;font-size:15px;font-family:'SFMono-Regular',Consolas,Menlo,monospace;
								color:#0B2E73;font-weight:600;">%s</p>
						</td>
					</tr>
				</table>
				<div style="text-align:center;margin:0 0 8px;">
					<a href="%s" style="display:inline-block;background:#1565C6;color:#ffffff;text-decoration:none;
						font-size:15px;font-weight:600;padding:14px 32px;border-radius:12px;">
						Se connecter
					</a>
				</div>
				"""
						.formatted(greetingName, escape(email), escape(rawPassword), loginUrl()));
	}

	private String resetCodeTemplate(String firstName, String code) {
		String greetingName = (firstName == null || firstName.isBlank()) ? "" : " " + escape(firstName);
		return baseLayout(
				"Réinitialisation du mot de passe",
				"""
				<p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#334155;">
					Bonjour%s,
				</p>
				<p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155;">
					Voici votre code de vérification pour réinitialiser votre mot de passe Harmonie-dev.
					Ce code expire dans 10 minutes.
				</p>
				<div style="text-align:center;margin:0 0 28px;">
					<span style="display:inline-block;background:#EAF2FD;border:1px solid #CFE1F8;
						border-radius:14px;padding:18px 36px;font-size:32px;font-weight:700;letter-spacing:.3em;
						color:#0B2E73;font-family:'SFMono-Regular',Consolas,Menlo,monospace;">%s</span>
				</div>
				<p style="margin:0;font-size:13px;line-height:1.6;color:#64748B;text-align:center;">
					Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail en toute sécurité.
				</p>
				"""
						.formatted(greetingName, escape(code)));
	}

	private String baseLayout(String heading, String bodyHtml) {
		return """
				<!DOCTYPE html>
				<html lang="fr">
				<head><meta charset="UTF-8"></head>
				<body style="margin:0;padding:32px 16px;background:#0B2E73;
					font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
					<table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;margin:0 auto;">
						<tr>
							<td style="background:linear-gradient(135deg,#0B2E73,#1565C6);border-radius:20px 20px 0 0;
								padding:32px 24px;text-align:center;">
								<img src="cid:%s" alt="Harmonie-dev" width="48" height="48"
									style="display:block;margin:0 auto 12px;border-radius:12px;background:#ffffff;padding:6px;" />
								<p style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">Harmonie-dev</p>
							</td>
						</tr>
						<tr>
							<td style="background:#ffffff;padding:36px 32px;">
								<h1 style="margin:0 0 20px;font-size:20px;font-weight:700;color:#0B2E73;">%s</h1>
								%s
							</td>
						</tr>
						<tr>
							<td style="background:#ffffff;border-radius:0 0 20px 20px;padding:0 32px 28px;">
								<p style="margin:0;font-size:12px;color:#94A3B8;text-align:center;border-top:1px solid #EEF2F7;
									padding-top:20px;">
									© %d Harmonie-dev — Facturation moderne. Cet e-mail vous a été envoyé automatiquement,
									merci de ne pas y répondre.
								</p>
							</td>
						</tr>
					</table>
				</body>
				</html>
				"""
				.formatted(LOGO_CID, escape(heading), bodyHtml, java.time.Year.now().getValue());
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
