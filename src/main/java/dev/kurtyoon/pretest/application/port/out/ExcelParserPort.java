package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;

import java.util.List;

public interface ExcelParserPort {
    List<OrderCommand> parse(byte[] excelData);
}
