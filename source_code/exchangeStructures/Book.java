package exchangeStructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import fills.Fill;
import messages.Cancel;
import messages.RestingOrderConfirmation;
import orderSpecs.ClientId;
import orderSpecs.ClientOrderId;
import orderSpecs.MarketId;
import orderSpecs.Price;
import orderSpecs.Quantity;
import orderSpecs.Side;
import orderTypes.RestingOrder;
import orderTypes.SweepingOrder;

public class Book
{
	// One Book has one market, one side, a list of pricelevels.
	private Exchange _exchange;
	private Market _bookMarket;
	private MarketId _marketId;
	private Side _side = Side.BUY;
	private Map<Price, PriceLevel> _priceLevels = new TreeMap<>(_side.getComparator());
	private Map<ClientOrderId, RestingOrder> listRestingOrder = new HashMap<>();
	

	// Constructor
	public Book()
	{

	}

	// Constructor to take Market and Side
	public Book(Market market, Side mySide)
	{
		this._side = mySide;
		this._bookMarket = market;
		this._marketId = market.getMarketId();
	}

	// Getter: get a list of PriceLevel in the book
	public Map<Price, PriceLevel> getPriceLevels()
	{
		return _priceLevels;
	}

	// Sweep method
	public void sweep(SweepingOrder sweepingOrder) throws Exception
	{
		// If current book doesn't have a pricelevel, add sweepingorder to the other book
		if (this._priceLevels.size() == 0 )
		{
			addRestingOrderToOtherSide(sweepingOrder);
		// If all the quantity is 0, then still need to add sweepingorder to the other book	
		} else if (sumQuantityMapPriceL(this.getPriceLevels())==0)
		{
			addRestingOrderToOtherSide(sweepingOrder);
			
			// Clear the map because all the quantity is 0
			this.getPriceLevels().clear();
		}
		else
		{
			// If it's a sell order
			if (sweepingOrder.getSide().equals(Side.SELL))
			{
				Price maxPrice = Collections.max(this.getPriceLevels().keySet());
				if (maxPrice.getValue() < sweepingOrder.getPrice().getValue())
				{
					// If maxPrice < sweepingPrice then we cannot find a fill, add sweepingorder to the other book
					addRestingOrderToOtherSide(sweepingOrder);
				} else if (maxPrice.getValue() >= sweepingOrder.getPrice().getValue())
				{
					// If maxPrice >= sweepingPrice. We will be able to find a fill/match
					addFillOrdersBidBook(sweepingOrder);
				}
			} else if (sweepingOrder.getSide().equals(Side.BUY))
			{
				Price minPrice = Collections.min(this.getPriceLevels().keySet());
				if (minPrice.getValue() > sweepingOrder.getPrice().getValue())
				{
					// If minPrice > sweepingPrice then we cannot find a fill, add sweepingorder to the other book
					addRestingOrderToOtherSide(sweepingOrder);
				} else if (minPrice.getValue() <= sweepingOrder.getPrice().getValue())
				{
					// If maxPrice <= sweepingPrice. We will be able to find a fill/match
					addFillOrdersOfferBook(sweepingOrder);
				}
			}
		}
	}

	// add fill in bid book
	public void addFillOrdersBidBook(SweepingOrder sweepingOrder) throws Exception
	{
		Iterator<Map.Entry<Price, PriceLevel>> it = this.getPriceLevels().entrySet().iterator();

		//Iterate through the map
		while (it.hasNext())
		{
			// iterator pair
			Map.Entry<Price, PriceLevel> p = it.next();

			// If any pricelevel price becomes less than sweepingorder's price and sweepingorder Q>0, add to other book.
			if (p.getKey().compareTo(sweepingOrder.getPrice()) < 0 && sweepingOrder.getQuantity().getValue() > 0)
			{
				addRestingOrderToOtherSide(sweepingOrder);
				break;
			}

			// If any pricelevel price is greater or equal to sweepingorder's price and sweepingorder Q>0, start to fill.
			if (p.getKey().compareTo(sweepingOrder.getPrice()) >= 0 && sweepingOrder.getQuantity().compareTo(new Quantity(0L)) > 0)
			{
				Iterator<RestingOrder> itrRestingOrder = this.getPriceLevels().get(p.getKey()).getOrders().iterator();

				//Iterate though restingOrder
				while (itrRestingOrder.hasNext())
				{
					RestingOrder restingOrder = itrRestingOrder.next();
					// If restingOrder has quantity 0, then remove this RestingOrder
					if (restingOrder.getQuantity().equals(new Quantity(0L)))
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						continue;
					}

					// If restingOrder does have Q>0, then reduce the minimum Q of RestingOrder and SweepingOrder
					Quantity reduceQ = findMinQuantity(restingOrder, sweepingOrder);
					
					// Generate two Fills: restingOrder fill and sweepingOrder fill.
					Fill restFillOrder = new Fill(restingOrder.getClientId(), sweepingOrder.getClientId(), restingOrder.getClientOrderId(), reduceQ);
					Fill sweepFillOrder = new Fill(sweepingOrder.getClientId(), restingOrder.getClientId(), sweepingOrder.getClientOrderId(), reduceQ);
					restingOrder.reduceQtyBy(reduceQ); 
					sweepingOrder.reduceQtyBy(reduceQ);
					this._bookMarket.getExchange().getComms().getFills().add(restFillOrder);
					this._bookMarket.getExchange().getComms().getFills().add(sweepFillOrder);
					
					// If after filling, the restingOrder has quantity 0, then remove this RestingOrder.
					if (restingOrder.getQuantity().equals(new Quantity(0L)) && sweepingOrder.getQuantity().getValue() > 0)
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						continue;
					}
					// If after filling, the restingOrder has Q>0 but sweepingOrder Q==0, then break the loop
					else if (restingOrder.getQuantity().getValue()>0 && sweepingOrder.getQuantity().getValue() == 0)
					{
						break;
					}
					// If both restingOrder and sweepingOrder Q==0, then remove restingOrder and break
					else
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						break;
					}
				}
				
				// If PriceLevel size == 0, remove this pricelevel.
				if (this.getPriceLevels().get(p.getKey()).getOrders().size() == 0)
				{
					it.remove();
				}

			}
		}
	}
	
	
	public void addFillOrdersOfferBook(SweepingOrder sweepingOrder) throws Exception
	{
		Iterator<Map.Entry<Price, PriceLevel>> it = this.getPriceLevels().entrySet().iterator();

		//Iterate through the map
		while (it.hasNext())
		{
			// iterator pair
			Map.Entry<Price, PriceLevel> p = it.next();

			// If any pricelevel price becomes more than sweepingorder's price and sweepingorder Q>0, add to other book.
			if (p.getKey().compareTo(sweepingOrder.getPrice()) > 0 && sweepingOrder.getQuantity().getValue() > 0)
			{
				addRestingOrderToOtherSide(sweepingOrder);
				break;
			}

			// If any pricelevel price is less or equal to sweepingorder's price and sweepingorder Q>0, start to fill.
			if (p.getKey().compareTo(sweepingOrder.getPrice()) <= 0 && sweepingOrder.getQuantity().compareTo(new Quantity(0L)) > 0)
			{
				Iterator<RestingOrder> itrRestingOrder = this.getPriceLevels().get(p.getKey()).getOrders().iterator();

				//Iterate though restingOrder
				while (itrRestingOrder.hasNext())
				{
					RestingOrder restingOrder = itrRestingOrder.next();
					// If restingOrder has quantity 0, then remove this RestingOrder
					if (restingOrder.getQuantity().equals(new Quantity(0L)))
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						continue;
					}

					// If restingOrder does have Q>0, then reduce the minimum Q of RestingOrder and SweepingOrder
					Quantity reduceQ = findMinQuantity(restingOrder, sweepingOrder);
					
					// Generate two Fills: restingOrder fill and sweepingOrder fill.
					Fill restFillOrder = new Fill(restingOrder.getClientId(), sweepingOrder.getClientId(), restingOrder.getClientOrderId(), reduceQ);
					Fill sweepFillOrder = new Fill(sweepingOrder.getClientId(), restingOrder.getClientId(), sweepingOrder.getClientOrderId(), reduceQ);
					restingOrder.reduceQtyBy(reduceQ); 
					sweepingOrder.reduceQtyBy(reduceQ);
					this._bookMarket.getExchange().getComms().getFills().add(restFillOrder);
					this._bookMarket.getExchange().getComms().getFills().add(sweepFillOrder);
							
					// If after filling, the restingOrder has quantity 0, then remove this RestingOrder.
					if (restingOrder.getQuantity().equals(new Quantity(0L)) && sweepingOrder.getQuantity().getValue() > 0)
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						continue;
					}
					// If after filling, the restingOrder has Q>0 but sweepingOrder Q==0, then break the loop
					else if (restingOrder.getQuantity().getValue()>0 && sweepingOrder.getQuantity().getValue() == 0)
					{
						break;
					}
					// If both restingOrder and sweepingOrder Q==0, then remove restingOrder and break
					else
					{
						// remove from pricelevel
						itrRestingOrder.remove();
						// remove from exchange restingorder list
						this._bookMarket.getExchange().getMapRestingOrder().remove(restingOrder.getClientOrderId(), restingOrder);
						break;
					}
				}
				
				// If PriceLevel size == 0, remove this pricelevel.
				if (this.getPriceLevels().get(p.getKey()).getOrders().size() == 0)
				{
					it.remove();
				}
			}
		}
	}

	public Quantity findMinQuantity(RestingOrder myRestingOrder, SweepingOrder mySweepingOrder) throws Exception
	{
		long minQuantity = 0;

		if (myRestingOrder.getQuantity().getValue() >= mySweepingOrder.getQuantity().getValue())
		{
			minQuantity = mySweepingOrder.getQuantity().getValue();
		} else
		{
			minQuantity = myRestingOrder.getQuantity().getValue();
		}
		Quantity minQ = new Quantity(minQuantity);
		return minQ;
	}

	public long sumQuantity(PriceLevel pLevel)
	{
		long sum = 0;
		for (RestingOrder r : pLevel.getOrders())
		{
			sum = sum + r.getQuantity().getValue();
		}
		return sum;
	}
	
	public long sumQuantityMapPriceL(Map<Price, PriceLevel> pLevel)
	{
		long sum = 0;
		Iterator<Map.Entry<Price, PriceLevel>> it = pLevel.entrySet().iterator();
		while (it.hasNext())
		{
			// iterator pair
			Map.Entry<Price, PriceLevel> p = it.next();
			sum = sum + sumQuantity(p.getValue());
		}

		return sum;
	}
	
	public void addRestingOrderToOtherSide(SweepingOrder sweepingOrder)
	{
		// Becomes a restingOrder
		RestingOrder newRestingOrder = new RestingOrder(sweepingOrder);
		this.getListRestingOrders().put(sweepingOrder.getClientOrderId(), newRestingOrder);
		PriceLevel newPriceLevel = new PriceLevel(sweepingOrder.getPrice(), newRestingOrder);
		

		// Store this restingOrder to the exchange's order map.
		this._bookMarket.getExchange().getMapRestingOrder().put(sweepingOrder.getClientOrderId(), newRestingOrder);

		// Store this restingOrder to the other side of the book.
		if (this.getOtherBook(_bookMarket, this).getPriceLevels().containsKey(newPriceLevel.getPrice()))
		{
			this.getOtherBook(_bookMarket, this).getPriceLevels().get(sweepingOrder.getPrice()).getOrders()
					.add(newRestingOrder);
		} else
		{
			// use "else" otherwise it will overwrite previous order
			this.getOtherBook(_bookMarket, this).getPriceLevels().put(sweepingOrder.getPrice(), newPriceLevel);
		}

		// Store this restingOrder to the RestingOrderConfirmation
		RestingOrderConfirmation newRestingOrderConfirm = new RestingOrderConfirmation(newRestingOrder);
		_bookMarket.getExchange().getComms().getRestingOrderConfirmations().addLast(newRestingOrderConfirm);

	}

	// Set book's market
	public void setMarket(Market market)
	{
		this._bookMarket = market;
	}

	// Set book's marketId
	public void setMarketId(MarketId _marketId)
	{
		this._marketId = _marketId;
	}

	// Get the other book for the same market.
	public Book getOtherBook(Market currentMarket, Book currentBook)
	{
		if (currentBook.getSide().equals(Side.BUY))
		{
			return currentMarket.getOfferBook();
		}
		return currentMarket.getBidBook();
	}

	// Get Side to see what's the type of the book.
	public Side getSide()
	{
		return this._side;
	}

	// Get list of restingOrder.
	public Map<ClientOrderId, RestingOrder> getListRestingOrders()
	{
		return this.listRestingOrder;
	}

	// Set Side
	public void setSide(Side side)
	{
		_side = side;
		_priceLevels = new TreeMap<>(side.getComparator());
	}

	// Get the opposite side
	public Side getOtherSide()
	{
		if (this.getSide().equals(Side.BUY))
		{
			return Side.SELL;
		}
		return Side.BUY;
	}

	public void cancel(Cancel cancelOrder)
	{
		ClientOrderId clientOrderId = cancelOrder.getClientOrderId();
		RestingOrder cancelRestingOrder = this.getListRestingOrders().get(clientOrderId);

		// Loop through Pricelevels in this Book
		// Iterator<Price> price = this.getPriceLevels().keySet().iterator();
		Iterator<Map.Entry<Price, PriceLevel>> t = this.getPriceLevels().entrySet().iterator();

		while (t.hasNext())
		// for (Price p : this.getPriceLevels().keySet())
		{
			Map.Entry<Price, PriceLevel> p = t.next();
			Iterator<RestingOrder> it = this.getPriceLevels().get(p.getKey()).getOrders().iterator();
			while (it.hasNext())
			{
				RestingOrder restingOrder = it.next();
				if (restingOrder.getClientOrderId().equals(clientOrderId))
				{
					it.remove();
				}
			}

			if (this.getPriceLevels().get(p.getKey()).getOrders().size() == 0)
				t.remove();

		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public Book(MarketId marketId)
	{
		_marketId = marketId;
	}

	// Set current book to the opposite side
	public void setOtherSide(Book newBook)
	{
		this._side = newBook.getSide();
	}

	public Exchange getExchange()
	{
		return _exchange;
	}


}


//if (sweepingOrder.getSide().equals(Side.SELL))
//{
//	Price maxPrice = Collections.max(this.getPriceLevels().keySet());
//	if (maxPrice.getValue() < sweepingOrder.getPrice().getValue() || this._priceLevels.size() == 0)
//	{
//		addRestingOrderToOtherSide(sweepingOrder);
//	} else if (maxPrice.getValue() >= sweepingOrder.getPrice().getValue())
//	{
//		addFillOrdersBidBook(sweepingOrder);
//	}
//} else if (sweepingOrder.getSide().equals(Side.BUY))
//{
//	Price minPrice = Collections.min(this.getPriceLevels().keySet());
//	if (minPrice.getValue() > sweepingOrder.getPrice().getValue() || this._priceLevels.size() == 0)
//	{
//		addRestingOrderToOtherSide(sweepingOrder);
//	} else if (minPrice.getValue() <= sweepingOrder.getPrice().getValue())
//	{
//		addFillOrdersOfferBook(sweepingOrder);
//	}
//}
//
