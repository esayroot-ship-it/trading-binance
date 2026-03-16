package org.example.tradingsvc.feign;

import org.example.tradingsvc.dto.MarketQuoteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tools.R;

@FeignClient(name = "market-svc")
public interface MarketQueryClient {

    @GetMapping("/api/market/price/{symbol}")
    R<MarketQuoteDTO> getQuote(@PathVariable("symbol") String symbol);
}
