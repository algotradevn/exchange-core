package exchange.core2.tests.examples;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import exchange.core2.core.ExchangeApi;
import exchange.core2.core.ExchangeCore;
import exchange.core2.core.IEventsHandler;
import exchange.core2.core.SimpleEventsProcessor;
import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.common.L2MarketData;
import exchange.core2.core.common.OrderAction;
import exchange.core2.core.common.OrderType;
import exchange.core2.core.common.SymbolType;
import exchange.core2.core.common.api.ApiAddUser;
import exchange.core2.core.common.api.ApiAdjustUserBalance;
import exchange.core2.core.common.api.ApiCancelOrder;
import exchange.core2.core.common.api.ApiMoveOrder;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.api.binary.BatchAddSymbolsCommand;
import exchange.core2.core.common.api.reports.SingleUserReportQuery;
import exchange.core2.core.common.api.reports.SingleUserReportResult;
import exchange.core2.core.common.api.reports.TotalCurrencyBalanceReportQuery;
import exchange.core2.core.common.api.reports.TotalCurrencyBalanceReportResult;
import exchange.core2.core.common.cmd.CommandResultCode;
import exchange.core2.core.common.config.ExchangeConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalExchangeCoreExample {

	@Test
	public void sampleTest() throws Exception {

		// simple async events handler
		final SimpleEventsProcessor eventsProcessor = new SimpleEventsProcessor(new IEventsHandler() {
			@Override
			public void tradeEvent(final TradeEvent tradeEvent) {
				System.out.println("Trade event: " + tradeEvent);
			}

			@Override
			public void reduceEvent(final ReduceEvent reduceEvent) {
				System.out.println("Reduce event: " + reduceEvent);
			}

			@Override
			public void rejectEvent(final RejectEvent rejectEvent) {
				System.out.println("Reject event: " + rejectEvent);
			}

			@Override
			public void commandResult(final ApiCommandResult commandResult) {
				System.out.println("Command result: " + commandResult);
			}

			@Override
			public void orderBook(final OrderBook orderBook) {
				System.out.println("OrderBook event: " + orderBook);
			}
		});

		// default exchange configuration
		final ExchangeConfiguration conf = ExchangeConfiguration.defaultBuilder().build();

		// build exchange core
		final ExchangeCore exchangeCore = ExchangeCore.builder()
				.resultsConsumer(eventsProcessor)
				.exchangeConfiguration(conf)
				.build();

		// start up disruptor threads
		exchangeCore.startup();

		// get exchange API for publishing commands
		final ExchangeApi api = exchangeCore.getApi();

		// currency code constants
		final int currencyCodeF1 = 11;
		final int currencyCodeVND = 15;

		// symbol constants
		final int symbolF1VND = 241;

		Future<CommandResultCode> future;

		// create symbol specification and publish it
		final CoreSymbolSpecification symbolSpecXbtLtc = CoreSymbolSpecification.builder()
				.symbolId(symbolF1VND) // symbol id
				.type(SymbolType.FUTURES_CONTRACT)
				.baseCurrency(currencyCodeF1) // base = satoshi (1E-8)
				.quoteCurrency(currencyCodeVND) // quote = litoshi (1E-8)
				.baseScaleK(1L) // 1 lot = 1M satoshi (0.01 BTC)
				.quoteScaleK(10_000L) // 1 price step = 10K litoshi
				.takerFee(10000L) // taker fee 1900 litoshi per 1 lot
				.makerFee(10000L) // maker fee 700 litoshi per 1 lot
				.build();

		future = api.submitBinaryDataAsync(new BatchAddSymbolsCommand(symbolSpecXbtLtc));
		System.out.println("BatchAddSymbolsCommand result: " + future.get());


		// create user uid=301
		future = api.submitCommandAsync(ApiAddUser.builder()
				.uid(301L)
				.build());

		System.out.println("ApiAddUser 1 result: " + future.get());


		// create user uid=302
		future = api.submitCommandAsync(ApiAddUser.builder()
				.uid(302L)
				.build());

		System.out.println("ApiAddUser 2 result: " + future.get());

		// first user deposits 20 LTC
		future = api.submitCommandAsync(ApiAdjustUserBalance.builder()
				.uid(301L)
				.currency(currencyCodeF1)
				.amount(5L)
				.transactionId(1L)
				.build());

		System.out.println("ApiAdjustUserBalance 1 result: " + future.get());

		// second user deposits 0.10 BTC
		future = api.submitCommandAsync(ApiAdjustUserBalance.builder()
				.uid(302L)
				.currency(currencyCodeVND)
				.amount(500_000_000L)
				.transactionId(2L)
				.build());

		System.out.println("ApiAdjustUserBalance 2 result: " + future.get());


		// first user places Good-till-Cancel Bid order
		// he assumes BTCLTC exchange rate 154 LTC for 1 BTC
		// bid price for 1 lot (0.01BTC) is 1.54 LTC => 1_5400_0000 litoshi => 10K * 15_400 (in price steps)
		future = api.submitCommandAsync(ApiPlaceOrder.builder()
				.uid(302L)
				.orderId(5001L)
				.price(10_550L) //
				.reservePrice(10_580L) // can move bid order up
				// to the 1.56 LTC,
				// without replacing it
				.size(2L) // order size is 12 lots
				.action(OrderAction.BID) // buy
				.orderType(OrderType.GTC) // Good-till-Cancel
				.symbol(symbolF1VND)
				.build());

		System.out.println("ApiPlaceOrder 1 result: " + future.get());


		// second user places Immediate-or-Cancel Ask (Sell) order
		// he assumes wost rate to sell 152.5 LTC for 1 BTC
		future = api.submitCommandAsync(ApiPlaceOrder.builder()
				.uid(301L)
				.orderId(5002L)
				.price(10_550L)
				.size(1L) // order size is 10 lots
				.action(OrderAction.ASK)
				.orderType(OrderType.IOC) // Immediate-or-Cancel
				.symbol(symbolF1VND)
				.build());

		System.out.println("ApiPlaceOrder 2 result: " + future.get());


		// request order book
		final CompletableFuture<L2MarketData> orderBookFuture = api
				.requestOrderBookAsync(symbolF1VND, 10);
		System.out.println("ApiOrderBookRequest result: " + orderBookFuture.get());


		// first user moves remaining order to price 1.53 LTC
		future = api.submitCommandAsync(ApiMoveOrder.builder()
				.uid(302L)
				.orderId(5001L)
				.newPrice(10_560L)
				.symbol(symbolF1VND)
				.build());

		System.out.println("ApiMoveOrder 2 result: " + future.get());

		// first user cancel remaining order
		future = api.submitCommandAsync(ApiCancelOrder.builder()
				.uid(302L)
				.orderId(5001L)
				.symbol(symbolF1VND)
				.build());

		System.out.println("ApiCancelOrder 2 result: " + future.get());

		// check balances
		final Future<SingleUserReportResult> report1 = api.processReport(new SingleUserReportQuery(301), 0);
		System.out.println("SingleUserReportQuery 1 accounts: " + report1.get().getAccounts());

		final Future<SingleUserReportResult> report2 = api.processReport(new SingleUserReportQuery(302), 0);
		System.out.println("SingleUserReportQuery 2 accounts: " + report2.get().getAccounts());

		// first user withdraws 0.10 BTC
		future = api.submitCommandAsync(ApiAdjustUserBalance.builder()
				.uid(301L)
				.currency(currencyCodeF1)
				.amount(-10_000_000L)
				.transactionId(3L)
				.build());

		System.out.println("ApiAdjustUserBalance 1 result: " + future.get());

		// check fees collected
		final Future<TotalCurrencyBalanceReportResult> totalsReport = api.processReport(new TotalCurrencyBalanceReportQuery(), 0);
		System.out.println(
				"LTC fees collected: " + totalsReport.get().getFees().get(currencyCodeVND));

	}
}
