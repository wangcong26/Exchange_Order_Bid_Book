package exchangeStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import messages.Cancel;
import messages.CancelRejected;
import messages.Cancelled;
import messages.RestingOrderConfirmation;
import orderSpecs.ClientOrderId;
import orderSpecs.MarketId;
import orderSpecs.Price;
import orderSpecs.Side;
import orderTypes.RestingOrder;
import orderTypes.SweepingOrder;

public class Exchange
{
	// In my design, each Exchange has multiple Markets using MarketId to indentify
	private Market _market;
	private MarketId _marketId;
	private Comms _comms = new Comms(); 
	private Map<MarketId, Market> _listMarkets = new HashMap<>();
	private Map<ClientOrderId, RestingOrder> _listRestingOrder = new HashMap<>();
	private Map<ClientOrderId, SweepingOrder> _listSweepingOrder = new HashMap<>();

	// Constructor
	Exchange()
	{

	}

	// Each exchange can have multiple markets. Each market means for one stock trading.
	// To add a market to my Exchange.
	public void addMarket(final Market market)
	{
		_market = market;
		_listMarkets.put(market.getMarketId(), market);

		// Set this market's bidbook to be in this market and marketId
		this.getMarket(market.getMarketId()).getBidBook().setMarket(market);
		this.getMarket(market.getMarketId()).getBidBook().setMarketId(market.getMarketId());
		this.getMarket(market.getMarketId()).getBidBook().setSide(Side.BUY);

		// Set this market's offerbook to be in this market and marketId
		this.getMarket(market.getMarketId()).getOfferBook().setMarket(market);
		this.getMarket(market.getMarketId()).getOfferBook().setMarketId(market.getMarketId());
		this.getMarket(market.getMarketId()).getOfferBook().setSide(Side.SELL);
	}
	
	// Sweep method used to sweep an order that comes into my Exchange
	public void sweep(SweepingOrder sweepingOrder) throws Exception
	{
		// If there is no such a market, create a market for this sweepingorder.
		if (!_listMarkets.keySet().contains(sweepingOrder.getMarketId()))
		{
			Market newMarket = new Market(this, sweepingOrder.getMarketId());
			_listMarkets.put(sweepingOrder.getMarketId(), newMarket);
			this.getMarket(sweepingOrder.getMarketId()).sweep(sweepingOrder);
		}

		// Otherwise, sweep this order.
		this.getMarket(sweepingOrder.getMarketId()).sweep(sweepingOrder);
	}
	
	// Cancel an order
	public void cancel(Cancel cancelOrder) throws Exception
	{
		// If the list of order doesn't contain cancelOrder's Id, then send cancelRejected message
		if (!this.getMapRestingOrder().containsKey(cancelOrder.getClientOrderId()))
		{
			// Store cancelReject to Comms
			CancelRejected cancelReject = new CancelRejected(cancelOrder.getClientId(), cancelOrder.getClientOrderId());
			this.getComms().getCancelRejections().addLast(cancelReject);
		} else
		{
			// Cancel the order from my Exchange using ClientOrderId
			_listRestingOrder.get(cancelOrder.getClientOrderId()).cancel();
			
			// Here is only to reduce the quantity!
			_listRestingOrder.get(cancelOrder.getClientOrderId()).getSweepingOrder().reduceQtyBy(_listRestingOrder.get(cancelOrder.getClientOrderId()).getSweepingOrder().getQuantity());
			
			// Remove from the list of RestingOrder
			_listRestingOrder.remove(cancelOrder.getClientOrderId());
			
			// Add to cancelled linkedList
			Cancelled c = new Cancelled(cancelOrder.getClientId(), cancelOrder.getClientOrderId());
			_comms.cancelled(c);
		}
	}


	// Get a market based on the marketId
	public Market getMarket(MarketId marketId)
	{
		return _listMarkets.get(marketId);
	}

	// Get a list of Markets stored in a map
	public Map<MarketId, Market> getListMarkets()
	{
		return _listMarkets;
	}

	// Get a resting order using unique key clientOrderId.
	public RestingOrder getOrder(ClientOrderId clientOrderId)
	{
		return _listRestingOrder.get(clientOrderId);
	}

	// Get a list of RestingOrder
	public Map<ClientOrderId, RestingOrder> getMapRestingOrder()
	{
		return _listRestingOrder;
	}

	// Get Comms
	public Comms getComms()
	{
		return _comms;
	}

	// Set Comms
	public void setComms(Comms _comms)
	{
		this._comms = _comms;
	}

	@Override
	public String toString()
	{
		return "Exchange [_comms=" + _comms + ", _listMarkets=" + _listMarkets + ", _listRestingOrder="
				+ _listRestingOrder + "]";
	}
	
}

//// Cancel an order
//public void cancel(Cancel cancelOrder)
//{
//	// If the list of order doesn't contain cancelOrder's Id, then send cancelRejected message
//	if (!this.getMapRestingOrder().containsKey(cancelOrder.getClientOrderId()))
//	{
//		// Store cancelReject to Comms
//		CancelRejected cancelReject = new CancelRejected(cancelOrder.getClientId(), cancelOrder.getClientOrderId());
//		this.getComms().getCancelRejections().addLast(cancelReject);
//	} else
//	{
//		// Remove the order from my Exchange using ClientOrderId
//		this.getMapRestingOrder().remove(cancelOrder.getClientOrderId());
//
//		// Store CancelationConfirmation to Comms
//		Cancelled canceledOrder = new Cancelled(cancelOrder.getClientId(), cancelOrder.getClientOrderId());
//		this.getComms().getCancelationConfirmations().addLast(canceledOrder);
//
//		// Then sweep each market in my exchange to cancel the order in the book because we only know ClientId and ClientOrderId
//		for (MarketId marketId : this.getListMarkets().keySet())
//		{
//			// Because we don't know where the cancel is so we have to loop through both bidbook and offerbook
//			this.getMarket(marketId).getBidBook().cancel(cancelOrder);
//			this.getMarket(marketId).getOfferBook().cancel(cancelOrder);
//		}
//	}
//}






