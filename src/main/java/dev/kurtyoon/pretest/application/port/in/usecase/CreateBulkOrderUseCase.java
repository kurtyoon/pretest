package dev.kurtyoon.pretest.application.port.in.usecase;

import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;

public interface CreateBulkOrderUseCase {

    /**
     * 엑셀 파일을 업로드하여 주문을 생성합니다.
     * @param excelData 엑셀 파일 데이터
     * @return 주문 생성 결과
     */
    BulkOrderResult execute(byte[] excelData);
}
