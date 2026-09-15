package com.harmoniedev.api.pdf;

import com.harmoniedev.api.common.dto.CurrencyAmount;
import com.harmoniedev.api.report.domain.dto.response.ReportOverviewResponse;
import com.harmoniedev.api.report.domain.dto.response.TopClientResponse;
import com.harmoniedev.api.report.domain.dto.response.TopServiceResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ReportPdfService {
	private static final Color NAVY = new Color(11, 46, 115);
	private static final Color MUTED = new Color(90, 100, 120);
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public byte[] generateReportPdf(
			ReportOverviewResponse overview, List<TopClientResponse> topClients, List<TopServiceResponse> topServices,
			String dateFrom, String dateTo, String currency) {
		String selected = currency == null || currency.isBlank() ? "TND" : currency;
		Document document = new Document(PageSize.A4, 40, 40, 40, 40);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, out);
			document.open();

			document.add(new Paragraph("Rapport d'activité", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY)));
			String range = (dateFrom != null ? dateFrom : "début") + " → " + (dateTo != null ? dateTo : "aujourd'hui")
					+ " · généré le " + LocalDate.now().format(DATE_FMT);
			document.add(new Paragraph(range, FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED)));
			document.add(new Paragraph(" "));

			Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, NAVY);
			Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
			Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);

			document.add(new Paragraph("Vue d'ensemble (" + selected + ")", sectionFont));
			PdfPTable overviewTable = new PdfPTable(2);
			overviewTable.setWidthPercentage(100);
			overviewTable.setSpacingBefore(8);
			addStat(overviewTable, "Revenus", money(overview.getTotalRevenue(), selected), labelFont, valueFont);
			addStat(overviewTable, "Dépenses", money(overview.getTotalExpenses(), selected), labelFont, valueFont);
			document.add(overviewTable);

			List<CurrencyAmount> otherRevenue = otherCurrencies(overview.getRevenueByCurrency(), selected);
			List<CurrencyAmount> otherExpenses = otherCurrencies(overview.getExpensesByCurrency(), selected);
			if (!otherRevenue.isEmpty() || !otherExpenses.isEmpty()) {
				document.add(new Paragraph(" "));
				document.add(new Paragraph("Autres devises", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED)));
				for (CurrencyAmount c : otherRevenue) {
					document.add(new Paragraph("Revenus — " + money(c.getAmount(), c.getCurrency()), labelFont));
				}
				for (CurrencyAmount c : otherExpenses) {
					document.add(new Paragraph("Dépenses — " + money(c.getAmount(), c.getCurrency()), labelFont));
				}
			}
			document.add(new Paragraph(" "));

			document.add(new Paragraph("Meilleurs clients (" + selected + ")", sectionFont));
			document.add(buildRankedTable(topClients.stream().map(c -> new String[] {c.getNom(), money(c.getTotal(), selected)}).toList()));
			document.add(new Paragraph(" "));

			document.add(new Paragraph("Services les plus vendus (" + selected + ")", sectionFont));
			document.add(buildRankedTable(topServices.stream().map(s -> new String[] {s.getName(), money(s.getTotalSold(), selected)}).toList()));

			document.close();
			return out.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to generate report PDF", ex);
		}
	}

	private String money(double amount, String currency) {
		return String.format(Locale.ROOT, "%.3f %s", amount, currency);
	}

	private List<CurrencyAmount> otherCurrencies(List<CurrencyAmount> amounts, String selected) {
		if (amounts == null) return List.of();
		return amounts.stream().filter(c -> !selected.equals(c.getCurrency())).toList();
	}

	private void addStat(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(com.lowagie.text.Rectangle.BOX);
		cell.setPadding(10);
		Paragraph p1 = new Paragraph(label, labelFont);
		Paragraph p2 = new Paragraph(value, valueFont);
		cell.addElement(p1);
		cell.addElement(p2);
		table.addCell(cell);
	}

	private PdfPTable buildRankedTable(List<String[]> rows) {
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(100);
		table.setSpacingBefore(8);
		Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
		if (rows.isEmpty()) {
			PdfPCell empty = new PdfPCell(new Phrase("Aucune donnée pour cette période.", rowFont));
			empty.setColspan(2);
			empty.setPadding(8);
			table.addCell(empty);
			return table;
		}
		int i = 1;
		for (String[] row : rows) {
			PdfPCell nameCell = new PdfPCell(new Phrase(i + ". " + row[0], rowFont));
			nameCell.setPadding(6);
			PdfPCell valueCell = new PdfPCell(new Phrase(row[1], rowFont));
			valueCell.setPadding(6);
			valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(nameCell);
			table.addCell(valueCell);
			i++;
		}
		return table;
	}
}
