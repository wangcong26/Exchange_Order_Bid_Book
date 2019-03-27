##Wrote three Classes to implement how buy and sell orders are handled in an exchange.

#1. How to run the code:
After importing my homework, the three Classes are under Package "exchangeStructures". The names are : Book, Exchange, and PriceLevel


#2. Explanation of the Classes:
1) Exchange:
In this Class, I stored main things like: multiple Markets, a list of RestingOrders. Also I added a Cancel method in the Exchange, to cancel a restingOder.

2) Book: 
This is my core Class in this homework. I stored a list of PriceLevel objects using a Map, a Market and an Exchange.  This way we can know where this Book is belonged to. Also, I added a sweep method in this Class, that's used to sweep the order in the corresponding book.

3) PriceLevel:
In this class, I store a Price together with a LinkedList<RestingOrder>


#3. Explanation of the Junit Test.
In the Junit package, you would see 7 parts and I printed out all the relevant output to visualize what's behind he scene. But I included his original big unit test file without touching anything, just for the purpose of backing it up. All these 8 file are passed.

	



