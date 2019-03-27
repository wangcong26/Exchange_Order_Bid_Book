package exchangeStructures;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import fills.Fill;
import messages.Cancel;
import messages.CancelRejected;
import orderSpecs.ClientId;
import orderSpecs.ClientOrderId;
import orderSpecs.MarketId;
import orderSpecs.Price;
import orderSpecs.Quantity;
import orderSpecs.Side;
import orderTypes.RestingOrder;
import orderTypes.SweepingOrder;

public class Test_Exchange_p1 extends junit.framework.TestCase
{

	/**
	 * One giant test of all exchange functions
	 */
	public void test1() throws Exception
	{
		System.out.println("Part 1 test ");
		System.out.println();
		// Create an exchange and add one market
		Exchange exchange = new Exchange();
		MarketId marketId0 = new MarketId("IBM");
		Market market = new Market(exchange, marketId0);
		exchange.addMarket(market);

		// Before we start, there should be no price levels
		// in either the bid book or the offer book of the
		// above market
		assertTrue(exchange.getMarket(marketId0).getOfferBook().getPriceLevels().size() == 0);
		assertTrue(exchange.getMarket(marketId0).getBidBook().getPriceLevels().size() == 0);
		System.out.println("Below is my current exchange: ");
		System.out.println(exchange);
		System.out.println();
//////////////////////////////////////////////////////////////////////////////////////////////////////

		// #1 Create first order
		// Create a client0 order
		ClientId clientId0 = new ClientId("Lee");
		ClientOrderId clientOrderId0 = new ClientOrderId("ABC");
		Side side0 = Side.BUY;
		Quantity quantity0 = new Quantity(1000L);
		Price price0 = new Price(1280000);
		SweepingOrder sweepingOrder = new SweepingOrder(clientId0, clientOrderId0, marketId0, side0, quantity0, price0);

		// Sweep the exchange with this order
		exchange.sweep(sweepingOrder);

		// No part of the first order to buy 1000 shares at $128
		// was executed because the offer book was empty. So
		// the entire order became a resting order in the bid
		// book at a price level of $128.

		// There should be one price level in the bid book
		assertTrue(exchange.getMarket(marketId0).getBidBook().getPriceLevels().size() == 1);
		// There should be no price levels in the offer book
		assertTrue(exchange.getMarket(marketId0).getOfferBook().getPriceLevels().size() == 0);
		// The exchange should know about this order
		assertTrue(exchange.getOrder(sweepingOrder.getClientOrderId()).equals(new RestingOrder(sweepingOrder)));
		// Print out the size of my pricelevel.
		System.out.println("Client0: Size of pricelevel in my bidbook is: "+exchange.getMarket(marketId0).getBidBook().getPriceLevels().size());
		
		
		// Make sure that the above sweeping order generated a resting order
		// Specify the price level we will examine
		Price priceOfPriceLevel = sweepingOrder.getPrice();
		// We want the first order at that price level
		int orderIndex = 0;
		assertTrue(exchange.getMarket(marketId0).getBidBook().getPriceLevels().get(priceOfPriceLevel).getOrders()
				.get(orderIndex).equals(new RestingOrder(sweepingOrder)));
		
		System.out.println(exchange.getMarket(marketId0).getBidBook().getPriceLevels().get(priceOfPriceLevel).getOrders().get(orderIndex));
		System.out.println();

		// Make sure that the market sent an alert to the client about the new resting order via the fake comms link
		RestingOrder restingOrder = exchange.getComms().getRestingOrderConfirmations().getLast().getRestingOrder();
		assertTrue(restingOrder.equals(new RestingOrder(sweepingOrder)));
		assertTrue(restingOrder.getQuantity().equals(new Quantity(1000)));
		System.out.println("RestingOrder is: "+restingOrder);
		System.out.println();
		
		// The message is sitting in comms just so we can check it. Now that we've checked it, we can remove it so it doesn't interfere with the rest of our tests.
		exchange.getComms().getRestingOrderConfirmations().removeLast();

		//See what's in the bid order book now.
		System.out.println("Current Bidbook has Pricelevel: ");
		Map<Price, PriceLevel> currentPriceLevel0 = new TreeMap<>();
		currentPriceLevel0 = exchange.getMarket(marketId0).getBidBook().getPriceLevels();
		for (Map.Entry<Price, PriceLevel> entry : currentPriceLevel0.entrySet())
		{
			System.out.println("Key=" + entry.getKey().getValue() + ", Value=" + entry.getValue());
		}
		System.out.println("End of part 1 test");
		System.out.println();
	}

}
