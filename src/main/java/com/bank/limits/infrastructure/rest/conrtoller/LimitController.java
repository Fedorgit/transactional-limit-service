package com.bank.limits.infrastructure.rest.conrtoller;

import com.bank.limits.application.service.ExpenseLimitService;
import com.bank.limits.infrastructure.rest.dto.limit.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.limit.SetNewLimitRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/limits")
@RequiredArgsConstructor
@Tag(name = "Limits", description = "Create and retrieve expense limits")
public class LimitController {

    private final ExpenseLimitService expenseLimitService;

    @PostMapping
    @Operation(
            summary = "Set an expense limit",
            description = "Creates a new limit for an account and expense category."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Limit successfully created",
                    content = @Content(schema = @Schema(implementation = LimitResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public LimitResponseDto setNewExpenseLimit(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details of the new expense limit",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SetNewLimitRequestDto.class))
            )
            @Valid @RequestBody SetNewLimitRequestDto request
    ) {
        return expenseLimitService.setNewExpenseLimit(request);
    }

    @GetMapping
    @Operation(
            summary = "Get account limits",
            description = "Returns a page of limits for the specified account. Pagination is configured using the page, size, and sort parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of expense limits"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public Page<LimitResponseDto> getLimits(
            @Parameter(description = "Account number to retrieve limits for", required = true)
            @RequestParam("account_from") String accountFrom,
            @ParameterObject Pageable pageable
    ) {
        return expenseLimitService.getLimits(accountFrom, pageable);
    }
}
