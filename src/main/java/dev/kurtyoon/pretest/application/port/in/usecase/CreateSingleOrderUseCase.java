package dev.kurtyoon.pretest.application.port.in.usecase;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;

public interface CreateSingleOrderUseCase {
    SingleOrderResult execute(OrderCommand command);
}
