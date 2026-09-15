package com.harmoniedev.api.pdf;

import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.company.domain.dto.response.CompanyResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceItemResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Server-side PDF rendering — mirrors the invoice layout used across the app. */
@Service
public class InvoicePdfService {
	private static final Color NAVY = new Color(11, 46, 115);
	private static final Color OCEAN = new Color(21, 101, 198);
	private static final Color MUTED = new Color(90, 100, 120);
	private static final Color LIGHT = new Color(237, 244, 253);
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATE_FMT_FR = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);

	public byte[] generateInvoicePdf(InvoiceResponse invoice, CompanyResponse company) {
		Document document = new Document(PageSize.A4, 40, 40, 40, 40);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
			Font smallMuted = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
			Font docTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, OCEAN);
			Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
			Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
			Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
			Font clientLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
			Font clientNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);

			PdfPTable header = new PdfPTable(2);
			header.setWidthPercentage(100);
			header.setWidths(new float[] {1.2f, 1f});

			PdfPCell brandCell = borderless();
			PdfPTable brandInner = new PdfPTable(2);
			brandInner.setWidths(new float[] {1f, 3.4f});
			Image logo = loadLogo(company);
			PdfPCell logoCell = borderless();
			logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			if (logo != null) logoCell.addElement(logo);
			brandInner.addCell(logoCell);
			PdfPCell nameCell = borderless();
			nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			nameCell.addElement(new Paragraph(company != null && company.getName() != null ? company.getName().toUpperCase() : "VOTRE ENTREPRISE", brandFont));
			brandInner.addCell(nameCell);
			brandCell.addElement(brandInner);
			header.addCell(brandCell);

			PdfPCell addressCell = borderless();
			addressCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			if (company != null) {
				if (company.getAddress() != null && !company.getAddress().isBlank()) addressCell.addElement(rightAligned(company.getAddress(), smallMuted));
				String line2 = joinNonBlank(company.getState(), company.getCountry());
				if (!line2.isBlank()) addressCell.addElement(rightAligned(line2, smallMuted));
				String line3 = joinNonBlank(company.getPhone(), company.getMatriculeFisc() != null && !company.getMatriculeFisc().isBlank() ? "MF " + company.getMatriculeFisc() : null);
				if (!line3.isBlank()) addressCell.addElement(rightAligned(line3, smallMuted));
			}
			header.addCell(addressCell);
			document.add(header);
			document.add(new Paragraph(" "));
			document.add(new Paragraph(" "));

			PdfPTable meta = new PdfPTable(2);
			meta.setWidthPercentage(100);
			meta.setWidths(new float[] {1.2f, 1f});

			PdfPCell docCell = borderless();
			Paragraph docType = new Paragraph(invoice.getStatus(), docTitleFont);
			docType.setSpacingAfter(8);
			docCell.addElement(docType);
			docCell.addElement(new Paragraph("Date : " + formatDateFr(invoice.getDate()), metaFont));
			docCell.addElement(new Paragraph("Numéro : # " + invoice.getNumber() + "/" + invoice.getYear(), metaFont));
			docCell.addElement(new Paragraph("Échéance : " + formatDateFr(invoice.getExpirationDate()), metaFont));
			meta.addCell(docCell);

			PdfPCell clientCell = borderless();
			clientCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			clientCell.addElement(rightAligned("Facturé à", clientLabelFont));
			clientCell.addElement(rightAligned(clientLabel(invoice.getClient()), clientNameFont));
			for (String extra : clientExtraLines(invoice.getClient())) {
				clientCell.addElement(rightAligned(extra, smallMuted));
			}
			meta.addCell(clientCell);
			document.add(meta);
			document.add(new Paragraph(" "));

			PdfPTable items = new PdfPTable(6);
			items.setWidthPercentage(100);
			items.setWidths(new float[] {1f, 3f, 1f, 1.4f, 1.6f, 1.4f});
			Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
			for (String h : new String[] {"Réf", "Article", "Qté", "Prix", "Taxe", "Total"}) {
				PdfPCell hc = new PdfPCell(new Phrase(h, headFont));
				hc.setBackgroundColor(NAVY);
				hc.setPadding(6);
				items.addCell(hc);
			}
			String symbol = invoice.getCurrency() != null ? invoice.getCurrency().getSymbol() : "";
			for (InvoiceItemResponse it : invoice.getItems()) {
				items.addCell(cell(it.getRef() == null || it.getRef().isBlank() ? "—" : it.getRef(), normalFont, Element.ALIGN_LEFT));
				PdfPCell articleCell = new PdfPCell();
				articleCell.setPadding(6);
				articleCell.setBackgroundColor(LIGHT);
				articleCell.addElement(new Paragraph(it.getArticle(), normalFont));
				if (it.getDescription() != null && !it.getDescription().isBlank()) {
					articleCell.addElement(new Paragraph(it.getDescription(), smallMuted));
				}
				items.addCell(articleCell);
				items.addCell(cell(quantity(it.getQuantity()), normalFont, Element.ALIGN_CENTER));
				items.addCell(cell(money(it.getPrice(), symbol), normalFont, Element.ALIGN_RIGHT));
				items.addCell(cell(it.getTaxName() == null || it.getTaxName().isBlank() ? "—" : it.getTaxName() + " (" + it.getTaxRate() + "%)", normalFont, Element.ALIGN_RIGHT));
				items.addCell(cell(money(it.getTotal(), symbol), boldFont, Element.ALIGN_RIGHT));
			}
			document.add(items);
			document.add(new Paragraph(" "));

			double totalHt = invoice.getSubtotal() - invoice.getTaxAmount() - invoice.getTimbre();
			PdfPTable totals = new PdfPTable(2);
			totals.setWidthPercentage(45);
			totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
			addTotalRow(totals, "Total HT", money(totalHt, symbol), normalFont, boldFont, false);
			addTotalRow(totals, "Total taxes", money(invoice.getTaxAmount(), symbol), normalFont, boldFont, false);
			addTotalRow(totals, "Timbre fiscal", money(invoice.getTimbre(), symbol), normalFont, boldFont, false);
			addTotalRow(totals, "Total TTC", money(invoice.getTotal(), symbol), boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, OCEAN), true);
			document.add(totals);
			document.add(new Paragraph(" "));

			if (invoice.getNote() != null && !invoice.getNote().isBlank()) {
				document.add(new Paragraph("Note : " + invoice.getNote(), smallMuted));
				document.add(new Paragraph(" "));
			}

			String currencyName = invoice.getCurrency() != null && invoice.getCurrency().getName() != null && !invoice.getCurrency().getName().isBlank()
					? invoice.getCurrency().getName()
					: symbol;
			String amountWords = NumberToWordsFr.convert((long) Math.floor(invoice.getTotal()));
			Paragraph amountInWords = new Paragraph(
					"La présente facture est arrêtée à la somme de " + amountWords + " " + currencyName + ".",
					FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY));
			document.add(amountInWords);

			Paragraph footer = new Paragraph("Facture générée par ordinateur, valable sans signature ni cachet.",
					FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED));
			footer.setAlignment(Element.ALIGN_CENTER);
			footer.setSpacingBefore(24);
			document.add(footer);
			Paragraph brand = new Paragraph("Généré avec Harmonie-dev", FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED));
			brand.setAlignment(Element.ALIGN_CENTER);
			document.add(brand);

			document.close();
			return out.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to generate invoice PDF", ex);
		}
	}

	public byte[] generateSummaryPdf(List<InvoiceResponse> invoices, String title) {
		Document document = new Document(PageSize.A4, 40, 40, 40, 40);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();
			document.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, NAVY)));
			document.add(new Paragraph(invoices.size() + " facture(s) · généré le " + LocalDate.now().format(DATE_FMT),
					FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED)));
			document.add(new Paragraph(" "));

			PdfPTable table = new PdfPTable(6);
			table.setWidthPercentage(100);
			Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
			for (String h : new String[] {"Numéro", "Client", "Date", "Statut", "Paiement", "Total"}) {
				PdfPCell hc = new PdfPCell(new Phrase(h, headFont));
				hc.setBackgroundColor(NAVY);
				hc.setPadding(6);
				table.addCell(hc);
			}
			Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
			for (InvoiceResponse inv : invoices) {
				String symbol = inv.getCurrency() != null ? inv.getCurrency().getSymbol() : "";
				table.addCell(cell((inv.getType().equals("Proforma") ? "PRO-" : "") + inv.getNumber() + "/" + inv.getYear(), rowFont, Element.ALIGN_LEFT));
				table.addCell(cell(clientLabel(inv.getClient()), rowFont, Element.ALIGN_LEFT));
				table.addCell(cell(formatDate(inv.getDate()), rowFont, Element.ALIGN_LEFT));
				table.addCell(cell(inv.getStatus(), rowFont, Element.ALIGN_LEFT));
				table.addCell(cell(inv.getPaymentStatus(), rowFont, Element.ALIGN_LEFT));
				table.addCell(cell(money(inv.getTotal(), symbol), rowFont, Element.ALIGN_RIGHT));
			}
			document.add(table);
			document.close();
			return out.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to generate summary PDF", ex);
		}
	}

	private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, boolean withRule) {
		PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
		labelCell.setBorder(withRule ? com.lowagie.text.Rectangle.TOP : com.lowagie.text.Rectangle.NO_BORDER);
		labelCell.setPaddingTop(6);
		labelCell.setPaddingBottom(4);
		PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
		valueCell.setBorder(withRule ? com.lowagie.text.Rectangle.TOP : com.lowagie.text.Rectangle.NO_BORDER);
		valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		valueCell.setPaddingTop(6);
		valueCell.setPaddingBottom(4);
		table.addCell(labelCell);
		table.addCell(valueCell);
	}

	private PdfPCell borderless() {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
		return cell;
	}

	private PdfPCell cell(String text, Font font, int align) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setPadding(6);
		cell.setHorizontalAlignment(align);
		cell.setBackgroundColor(LIGHT);
		return cell;
	}

	private Paragraph rightAligned(String text, Font font) {
		Paragraph p = new Paragraph(new Chunk(text, font));
		p.setAlignment(Element.ALIGN_RIGHT);
		return p;
	}

	private String clientLabel(ClientResponse client) {
		if (client == null) return "";
		if (client.getPerson() != null) {
			return (client.getPerson().getPrenom() + " " + client.getPerson().getNom()).trim();
		}
		if (client.getEntreprise() != null) {
			return client.getEntreprise().getNom();
		}
		return "";
	}

	private List<String> clientExtraLines(ClientResponse client) {
		if (client == null) return List.of();
		if (client.getPerson() != null) {
			var p = client.getPerson();
			return java.util.stream.Stream.of(
					p.getCin() != null && !p.getCin().isBlank() ? "CIN : " + p.getCin() : null,
					p.getAdresse())
					.filter(s -> s != null && !s.isBlank())
					.toList();
		}
		if (client.getEntreprise() != null) {
			var e = client.getEntreprise();
			return java.util.stream.Stream.of(
					e.getFisc() != null && !e.getFisc().isBlank() ? "MF : " + e.getFisc() : null,
					e.getAdresse())
					.filter(s -> s != null && !s.isBlank())
					.toList();
		}
		return List.of();
	}

	private String joinNonBlank(String a, String b) {
		if (a == null || a.isBlank()) return b == null ? "" : b;
		if (b == null || b.isBlank()) return a;
		return a + ", " + b;
	}

	private String formatDate(String isoDate) {
		try {
			return LocalDate.parse(isoDate).format(DATE_FMT);
		} catch (Exception ex) {
			return isoDate == null ? "" : isoDate;
		}
	}

	private String formatDateFr(String isoDate) {
		try {
			return LocalDate.parse(isoDate).format(DATE_FMT_FR);
		} catch (Exception ex) {
			return isoDate == null ? "" : isoDate;
		}
	}

	private Image loadLogo(CompanyResponse company) {
		if (company == null || company.getLogoUrl() == null || company.getLogoUrl().isBlank()) return null;
		try {
			Image image = Image.getInstance(URI.create(company.getLogoUrl()).toURL());
			image.scaleToFit(46, 46);
			return image;
		} catch (Exception ex) {
			return null;
		}
	}

	private String money(double value, String symbol) {
		return String.format(Locale.ROOT, "%.3f %s", value, symbol);
	}

	private String quantity(double value) {
		return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
	}
}
