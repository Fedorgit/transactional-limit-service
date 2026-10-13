package com.bank.limits.infrastructure.rest.conrtoller;

import com.bank.limits.application.service.TransactionService;
import com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Execute transactions and retrieve limit breaches")
public class TransactionsController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(
            summary = "Execute a transaction",
            description = "Checks the transaction against the applicable limit and returns whether the limit was exceeded."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction processed",
                    content = @Content(schema = @Schema(implementation = ExecutedTransactionResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public ExecutedTransactionResponseDto executeTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Details of the transaction to execute",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TransactionRequestDto.class))
            )
            @RequestBody TransactionRequestDto request
    ) {
        return transactionService.executeTransaction(request);
    }

    @GetMapping("/exceeded")
    @Operation(
            summary = "Get transactions that exceeded the limit",
            description = "Returns a page of transactions that exceeded the limit for the specified account. " +
                    "Pagination is configured using the page, size, and sort parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of transactions that exceeded the limit"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public Page<ExceededTransactionWithLimitDto> getExceededTransactions(
            @Parameter(description = "Account number to retrieve limit breaches for", required = true)
            @RequestParam("account_from") String accountFrom,
            @ParameterObject Pageable pageable
    ) {
        return transactionService.getExceededTransactions(accountFrom, pageable);
    }
}
