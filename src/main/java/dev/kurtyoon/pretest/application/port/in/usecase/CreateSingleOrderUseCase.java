package dev.kurtyoon.pretest.application.port.in.usecase;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;

public interface CreateSingleOrderUseCase {

    /**
     * 주문을 생성합니다.
     * @param command 주문 요청
     * @return 주문 생성 결과
     */
    SingleOrderResult execute(OrderCommand command);
}
