package dev.kurtyoon.pretest.application.port.in.usecase;

import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;

public interface CreateBulkOrderUseCase {
    BulkOrderResult execute(byte[] excelData);
}
