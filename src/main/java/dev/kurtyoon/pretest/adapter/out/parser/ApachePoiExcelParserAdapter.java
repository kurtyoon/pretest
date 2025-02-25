package dev.kurtyoon.pretest.adapter.out.parser;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.port.out.ExcelParserPort;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Adapter
public class ApachePoiExcelParserAdapter implements ExcelParserPort {

    private static final Logger log = LoggerUtils.getLogger(ApachePoiExcelParserAdapter.class);

    @Override
    public List<OrderCommand> parse(byte[] excelData) {
        List<OrderCommand> results = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData))) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null) continue;

                String customerName = getStringCellValue(row.getCell(0));
                String customerAddress = getStringCellValue(row.getCell(1));

                Long productId = (long) row.getCell(2).getNumericCellValue();
                String productName = getStringCellValue(row.getCell(3));
                int quantity = (int) row.getCell(4).getNumericCellValue();

                List<OrderItemCommand> items = new ArrayList<>();
                items.add(new OrderItemCommand(productId, productName, quantity));

                OrderCommand cmd = new OrderCommand(customerName, customerAddress, items);
                results.add(cmd);
            }
        } catch (Exception e) {
            log.error("Excel 파일을 읽는 중 예외 발생 : {}", e.getMessage());
        }

        return results;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";

        return cell.getStringCellValue().trim();
    }
}
