package com.smartshop.features.document.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartshop.features.purchase.dto.PurchaseItemResponse;
import com.smartshop.features.purchase.dto.PurchaseResponse;
import com.smartshop.features.purchase.service.PurchaseService;
import com.smartshop.features.sale.dto.SaleItemResponse;
import com.smartshop.features.sale.dto.SaleResponse;
import com.smartshop.features.sale.service.SaleService;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.shop.repository.ShopRepository;
import com.smartshop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUB_FONT = new Font(Font.HELVETICA, 9);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Color HEADER_BG = new Color(40, 53, 68);
    private static final Color TOTAL_BG = new Color(235, 238, 241);

    private final SaleService saleService;
    private final PurchaseService purchaseService;
    private final ShopRepository shopRepository;

    public byte[] saleInvoice(UUID saleId) {
        SaleResponse sale = saleService.get(saleId);
        Shop shop = shopRepository.findById(sale.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", sale.getShopId()));
        List<ItemRow> rows = new ArrayList<>();
        for (SaleItemResponse item : sale.getItems()) {
            rows.add(new ItemRow(item.getProductName(), item.getSku(), item.getQuantity(),
                    item.getUnitPrice(), item.getVatRate(), item.getVatAmount(), item.getLineTotal()));
        }
        return buildPdf(doc -> {
            writeHeader(doc, shop, "SALES INVOICE");
            writeMeta(doc, sale.getInvoiceNumber(), sale.getBillDate(), sale.getBranchName(),
                    "Customer", sale.getCustomerName(),
                    "Payment", sale.getPaymentStatus() + " / " + sale.getPaymentMethod(),
                    "Cashier", sale.getCashierName());
            writeItemsTable(doc, rows, "Unit Price");
            writeTotals(doc, sale.getSubtotal(), sale.getDiscountAmount(),
                    sale.getTaxableAmount(), sale.getVatAmount(), sale.getTotalAmount());
            writeRemarks(doc, sale.getRemarks());
        });
    }

    public byte[] purchaseOrder(UUID purchaseId) {
        PurchaseResponse purchase = purchaseService.get(purchaseId);
        Shop shop = shopRepository.findById(purchase.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop", purchase.getShopId()));
        List<ItemRow> rows = new ArrayList<>();
        for (PurchaseItemResponse item : purchase.getItems()) {
            rows.add(new ItemRow(item.getProductName(), item.getSku(), item.getQuantity(),
                    item.getUnitCost(), item.getVatRate(), item.getVatAmount(), item.getLineTotal()));
        }
        return buildPdf(doc -> {
            writeHeader(doc, shop, "PURCHASE ORDER");
            writeMeta(doc, purchase.getPurchaseNumber(), purchase.getPurchaseDate(), purchase.getBranchName(),
                    "Supplier", purchase.getSupplierName(),
                    "Payment", purchase.getPaymentStatus() == null ? "-" : purchase.getPaymentStatus().name(),
                    "Created by", purchase.getCreatedByName());
            writeItemsTable(doc, rows, "Unit Cost");
            writeTotals(doc, purchase.getSubtotal(), purchase.getDiscountAmount(),
                    purchase.getTaxableAmount(), purchase.getVatAmount(), purchase.getTotalAmount());
            writeRemarks(doc, purchase.getNotes());
        });
    }

    private byte[] buildPdf(DocumentWriter writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            writer.write(document);
        } catch (DocumentException e) {
            log.error("Failed to generate PDF document", e);
            throw new IllegalStateException("Failed to generate PDF document", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void writeHeader(Document doc, Shop shop, String title) throws DocumentException {
        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);
        head.setWidths(new float[]{3f, 1f});
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph(shop.getName(), TITLE_FONT));
        left.addElement(new Paragraph("PAN/VAT: " + nvl(shop.getPanVatNumber()), SUB_FONT));
        StringBuilder contact = new StringBuilder();
        if (notBlank(shop.getAddress())) {
            contact.append("Address: ").append(shop.getAddress());
        }
        if (notBlank(shop.getPhone())) {
            if (!contact.isEmpty()) {
                contact.append("   ");
            }
            contact.append("Phone: ").append(shop.getPhone());
        }
        if (notBlank(shop.getEmail())) {
            if (!contact.isEmpty()) {
                contact.append("   ");
            }
            contact.append("Email: ").append(shop.getEmail());
        }
        if (!contact.isEmpty()) {
            left.addElement(new Paragraph(contact.toString(), SUB_FONT));
        }
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (notBlank(shop.getLogoUrl())) {
            try {
                Image logo = Image.getInstance(new URL(shop.getLogoUrl()));
                logo.scaleToFit(70f, 70f);
                right.addElement(logo);
            } catch (Exception ignored) {
                right.addElement(new Paragraph(" "));
            }
        }
        head.addCell(left);
        head.addCell(right);
        doc.add(head);
        doc.add(new Paragraph(" "));
        Paragraph titleP = new Paragraph(title, TITLE_FONT);
        titleP.setAlignment(Element.ALIGN_CENTER);
        doc.add(titleP);
        doc.add(new Paragraph(" "));
    }

    private void writeMeta(Document doc, String documentNo, LocalDate date, String branch,
                           String partyLabel, String partyName, String paymentLabel, String payment,
                           String createdLabel, String createdBy) throws DocumentException {
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        addMetaCell(meta, "Document No.", nvl(documentNo));
        addMetaCell(meta, "Date", String.valueOf(date));
        addMetaCell(meta, "Branch", nvl(branch));
        addMetaCell(meta, partyLabel, nvl(partyName));
        addMetaCell(meta, paymentLabel, nvl(payment));
        addMetaCell(meta, createdLabel, nvl(createdBy));
        doc.add(meta);
        doc.add(new Paragraph(" "));
    }

    private void addMetaCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);
        Paragraph p = new Paragraph(label, new Font(Font.HELVETICA, 7, Font.BOLD, Color.GRAY));
        p.add(new Chunk("\n" + nvl(value), CELL_FONT));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void writeItemsTable(Document doc, List<ItemRow> rows, String priceLabel) throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{32, 12, 12, 12, 12, 10, 12});
        addHeader(table, "Product");
        addHeader(table, "SKU");
        addHeader(table, "Qty");
        addHeader(table, priceLabel);
        addHeader(table, "VAT %");
        addHeader(table, "VAT Amt");
        addHeader(table, "Line Total");
        int rowNo = 1;
        for (ItemRow row : rows) {
            addCell(table, rowNo++);
            addCell(table, row.productName);
            addCell(table, nvl(row.sku));
            addCell(table, amount(row.quantity));
            addCell(table, amount(row.unitAmount));
            addCell(table, amount(row.vatRate));
            addCell(table, amount(row.vatAmount));
            addCell(table, amount(row.lineTotal));
        }
        doc.add(table);
    }

    private void writeTotals(Document doc, BigDecimal subtotal, BigDecimal discount,
                             BigDecimal taxable, BigDecimal vat, BigDecimal total) throws DocumentException {
        doc.add(new Paragraph(" "));
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{1f, 1f});
        addTotalRow(totals, "Subtotal", subtotal);
        if (discount != null && discount.compareTo(BigDecimal.ZERO) != 0) {
            addTotalRow(totals, "Discount", discount.negate());
        }
        if (taxable != null) {
            addTotalRow(totals, "Taxable Amount", taxable);
        }
        if (vat != null) {
            addTotalRow(totals, "VAT Amount", vat);
        }
        PdfPCell totalLabel = new PdfPCell(new Phrase("GRAND TOTAL (Rs.)", SECTION_FONT));
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(TOTAL_BG);
        totalLabel.setBorder(Rectangle.BOX);
        totalLabel.setPadding(8);
        PdfPCell totalValue = new PdfPCell(new Phrase(amount(total), CELL_BOLD_FONT));
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setBackgroundColor(TOTAL_BG);
        totalValue.setBorder(Rectangle.BOX);
        totalValue.setPadding(8);
        totals.addCell(totalLabel);
        totals.addCell(totalValue);
        doc.add(totals);
    }

    private void writeRemarks(Document doc, String remarks) throws DocumentException {
        doc.add(new Paragraph(" "));
        String text = nvl(remarks);
        if (text.isBlank()) {
            text = "This is a system-generated document.";
        }
        doc.add(new Paragraph("Remarks / Notes: " + text, SUB_FONT));
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_BOLD_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, int number) {
        PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(number), CELL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal value) {
        PdfPCell l = new PdfPCell(new Phrase(label, CELL_FONT));
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(3);
        PdfPCell v = new PdfPCell(new Phrase(amount(value), CELL_FONT));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);
        table.addCell(l);
        table.addCell(v);
    }

    public static String amount(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String nvl(String value) {
        return value == null ? "-" : value;
    }

    @FunctionalInterface
    private interface DocumentWriter {
        void write(Document doc) throws DocumentException;
    }

    private record ItemRow(String productName, String sku, BigDecimal quantity, BigDecimal unitAmount,
                           BigDecimal vatRate, BigDecimal vatAmount, BigDecimal lineTotal) {
    }
}