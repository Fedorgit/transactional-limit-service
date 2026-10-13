package com.bank.limits.infrastructure.rest.external;

import com.bank.limits.infrastructure.rest.external.dto.ExchangesFromApiDto;

import java.util.List;

public interface ExternalApiClient {

    public List<ExchangesFromApiDto> doRequest(String baseCurrency, List<String> toCurrency);

}
