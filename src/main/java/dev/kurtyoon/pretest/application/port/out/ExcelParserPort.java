package dev.kurtyoon.pretest.application.port.out;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;

import java.util.List;

public interface ExcelParserPort {

    /**
     * 엑셀 파일을 파싱하여 주문 요청 목록을 반환합니다.
     * @param excelData 엑셀 파일 데이터
     * @return 주문 요청 목록
     */
    List<OrderCommand> parse(byte[] excelData);
}
