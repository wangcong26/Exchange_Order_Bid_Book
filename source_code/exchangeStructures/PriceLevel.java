package exchangeStructures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import orderSpecs.Price;
import orderTypes.RestingOrder;

public class PriceLevel
{

	// Fields
	Price price;
	LinkedList<RestingOrder> _listOrder = new LinkedList<RestingOrder>();

	// Constructor
	PriceLevel()
	{

	}

	// Constructor to get a Price and a restingOrder
	PriceLevel(Price aPrice, RestingOrder restingOrder)
	{
		price = aPrice;
		_listOrder.add(restingOrder);
	}

	// Constructor to get a Price and a list of of orders
	PriceLevel(Price aPrice, LinkedList<RestingOrder> listRestingOrder)
	{
		price = aPrice;
		_listOrder = listRestingOrder;
	}

	// Method: Get the list of restingOrders in the book.
	public LinkedList<RestingOrder> getOrders()
	{
		return this._listOrder;
	}
	
	// Method: Get Price.
	public Price getPrice()
	{
		return price;
	}
	
	// Method: Add a new restingorder to the pricelevel
	public void addRestingOrder(RestingOrder newRestingOrder)
	{
		this._listOrder.add(newRestingOrder);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setOrders(LinkedList<RestingOrder> _listOrder)
	{
		this._listOrder = _listOrder;
	}

	@Override
	public String toString()
	{
		return "PriceLevel [_listOrder=" + _listOrder + "]";
	}
	

}
