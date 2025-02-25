package dev.kurtyoon.pretest.adapter.out.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.request.OrderItemCommand;
import dev.kurtyoon.pretest.application.port.out.ExcelParserPort;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.annotation.Adapter;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Adapter
public class ApachePoiExcelParserAdapter implements ExcelParserPort {

    private static final Logger log = LoggerUtils.getLogger(ApachePoiExcelParserAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

                String productJson = getStringCellValue(row.getCell(2));

                List<OrderItemCommand> items = parseProductJson(productJson);

                OrderCommand cmd = new OrderCommand(customerName, customerAddress, items);
                results.add(cmd);
            }
        } catch (Exception e) {
            log.error("Excel 파일을 읽는 중 예외 발생 : {}", e.getMessage());
        }

        return results;
    }

    private List<OrderItemCommand> parseProductJson(String productJson) {
        try {
            return objectMapper.readValue(productJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.JSON_PARSING_ERROR);
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";

        return cell.getStringCellValue().trim();
    }
}
