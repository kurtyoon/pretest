package dev.kurtyoon.pretest.adapter.in.controller;

import dev.kurtyoon.pretest.application.dto.request.OrderCommand;
import dev.kurtyoon.pretest.application.dto.response.BulkOrderResult;
import dev.kurtyoon.pretest.application.dto.response.SingleOrderResult;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateBulkOrderUseCase;
import dev.kurtyoon.pretest.application.port.in.usecase.CreateSingleOrderUseCase;
import dev.kurtyoon.pretest.core.dto.ResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateSingleOrderUseCase createSingleOrderUseCase;
    private final CreateBulkOrderUseCase createBulkOrderUseCase;

    public OrderController(
            CreateSingleOrderUseCase createSingleOrderUseCase,
            CreateBulkOrderUseCase createBulkOrderUseCase
    ) {
        this.createSingleOrderUseCase = createSingleOrderUseCase;
        this.createBulkOrderUseCase = createBulkOrderUseCase;
    }

    @PostMapping("/single")
    public ResponseDto<SingleOrderResult> createSingleOrder(@RequestBody @Valid OrderCommand command) {
        return ResponseDto.created(createSingleOrderUseCase.execute(command));
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDto<BulkOrderResult> createBulkOrder(@RequestPart("file") MultipartFile file) throws IOException {
        return ResponseDto.ok(createBulkOrderUseCase.execute(file.getBytes()));
    }
}
