package com.vmware.tanzu.demos.sta.tradingagent.bid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.agent.strategy", havingValue = "artemis-agent")
class ArtemisBidAgent implements BidAgent {
    private final Logger logger = LoggerFactory.getLogger(ArtemisBidAgent.class);

    static Map<String, Integer> currentStocks = new HashMap<>();

    @Override
    public List<BidAgentRequest> execute(Context ctx) {

        // Sort input stocks against price.
        final List<Stock> sortedStocks = new ArrayList<>(ctx.stocks());
        List<Stock> buyingStocks = new ArrayList<>();
        List<Stock> sellingStocks = new ArrayList<>();

        Map<String, Double> ma30 = new HashMap<>();
        Map<String, Double> ma50 = new HashMap<>();
        Map<String, Integer> stockAmount = new HashMap<>();
        for(Stock stock:sortedStocks){
            double sum30 = 0;
            double sum50 = 0;
            StockPrice[] prices = ctx.stockPrices().get(stock.symbol());
            for(int i=0;i<30;i++) {
                sum30 += prices[prices.length - i].price();
            }
            for(int i=0;i<50;i++) {
                sum30 += prices[prices.length - i].price();
            }
            ma30.put(stock.symbol(),sum30/30);
            ma50.put(stock.symbol(),sum50/50);
            if(ma30.get(stock.symbol())>stock.price().doubleValue()){
                buyingStocks.add(stock);
            }
            if(ma50.get(stock.symbol())<=stock.price().doubleValue()){
                sellingStocks.add(stock);
            }
        }

        for(var stock:buyingStocks){
            int stocks = (int)(ctx.userBalance().doubleValue() / (3*buyingStocks.size()*stock.price().doubleValue()));
            stockAmount.put(stock.symbol(),stocks);
            currentStocks.put(stock.symbol(),currentStocks.put(stock.symbol(),stocks+currentStocks.get(stock.symbol())));
        }

        for(var stock:sellingStocks){
            int stocks = (int)(ctx.userBalance().doubleValue() / (3*stock.price().doubleValue()));
            stockAmount.put(stock.symbol(),currentStocks.get(stock.symbol()));
            currentStocks.put(stock.symbol(),0);
        }

        List<BidAgentRequest> bidAgentRequests = new ArrayList<>();

        for(Stock stock:sellingStocks){
            bidAgentRequests.add(new BidAgentRequest(stock.symbol(),-stockAmount.get(stock.symbol())));
        }

        for(Stock stock:buyingStocks){
            bidAgentRequests.add(new BidAgentRequest(stock.symbol(),stockAmount.get(stock.symbol())));
        }
        return bidAgentRequests;
    }

    @Override
    public String toString() {
        return "ARTEMIS_AGENT";
    }
}
