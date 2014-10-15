package org.jackho.stockGwt.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jackho.stockGwt.shared.FieldVerifier;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockGwt implements EntryPoint {
	/*********************** stock watch *********************************/
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable stocksFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addStockButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	// hold the list of stock symbols
	private ArrayList<String> stocks = new ArrayList<String>();
	// data & price refresh interval time .
	private static final int REFRESH_INTERVAL = 5000; // ms
	// log
	private static Logger rootLogger = Logger.getLogger("");

	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		
		/**************** Greeting ******************************/
		final Button sendButton = new Button("Send");
		final TextBox nameField = new TextBox();
		nameField.setText("GWT User");
		final Label errorLabel = new Label();

		// We can add style names to widgets
		sendButton.addStyleName("sendButton");

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		
		RootPanel.get("sendButtonContainer").add(sendButton);
		RootPanel.get("nameFieldContainer").add(nameField);
		
		RootPanel.get("errorLabelContainer").add(errorLabel);

		// Focus the cursor on the name field when the app loads
		nameField.setFocus(true);
		nameField.selectAll();

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		
		/************* stock watch ***************/
		
		// Create table for stock data.
	    stocksFlexTable.setText(0, 0, "Symbol");
	    stocksFlexTable.setText(0, 1, "Price");
	    stocksFlexTable.setText(0, 2, "Change");
	    stocksFlexTable.setText(0, 3, "Remove");
	    // 在每個cell裡面都加上padding
	    stocksFlexTable.setCellPadding(20);
	    
	    
	    /*************** style FlexTable *****************/
	    // Add styles to elements in the stock list table. 第一個row => Symbol Price Change Remove
	    stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
	    // 整個FlexTable 都加上css => .watchList
	    stocksFlexTable.addStyleName("watchList");
	    
	    
	    /*************** end style FlexTable *****************/
	    
	    // Assemble Add Stock panel.
	    addPanel.add(newSymbolTextBox);
	    addPanel.add(addStockButton);
	    addPanel.addStyleName("addPanel");

	    // Assemble Main panel.
	    mainPanel.add(stocksFlexTable);
	    mainPanel.add(addPanel);
	    mainPanel.add(lastUpdatedLabel);

	    // Associate the Main panel with the HTML host page.
	    RootPanel.get("stockList").add(mainPanel);

	    // Move cursor focus to the input box.
	    newSymbolTextBox.setFocus(true);
		
		
		
	 // Setup timer to refresh list automatically.
	    Timer refreshTimer = new Timer() {
	      @Override
	      public void run() {
	        refreshWatchList();
	      }
	    };
	    //根據 REFRESH_INTERVAL : 5000ms 一直repeat call run
	    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
		
		
		
		
		
		
		
		
		
		
		
	    /************************* Greeting handler ************************************/
		
		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				sendButton.setEnabled(true);
				sendButton.setFocus(true);
			}
		});

		// Create a handler for the sendButton and nameField
		class MyHandler implements ClickHandler, KeyUpHandler {
			/**
			 * Fired when the user clicks on the sendButton.
			 */
			public void onClick(ClickEvent event) {
				sendNameToServer();
			}

			/**
			 * Fired when the user types in the nameField.
			 */
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					sendNameToServer();
				}
			}

			/**
			 * Send the name from the nameField to the server and wait for a response.
			 */
			private void sendNameToServer() {
				// First, we validate the input.
				errorLabel.setText("");
				String textToServer = nameField.getText();
				if (!FieldVerifier.isValidName(textToServer)) {
					errorLabel.setText("Please enter at least four characters");
					return;
				}

				// Then, we send the input to the server.
				sendButton.setEnabled(false);
				textToServerLabel.setText(textToServer);
				serverResponseLabel.setText("");
				greetingService.greetServer(textToServer,
						new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
								// Show the RPC error message to the user
								dialogBox
										.setText("Remote Procedure Call - Failure");
								serverResponseLabel
										.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							public void onSuccess(String result) {
								dialogBox.setText("Remote Procedure Call");
								serverResponseLabel
										.removeStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(result);
								dialogBox.center();
								closeButton.setFocus(true);
							}
						});
			}
		}

		// Add a handler to send the name to the server
		MyHandler handler = new MyHandler();
		sendButton.addClickHandler(handler);
		nameField.addKeyUpHandler(handler);
		
		/************* stock watch handler ***************/
		addStockButton.addClickHandler(new ClickHandler() {
		      public void onClick(ClickEvent event) {
		        addStock();
		   }
		});
		// Listen for keyboard events in the input box.
	    newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
	      public void onKeyDown(KeyDownEvent event) {
	        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
	          addStock();
	        }
	      }
	    });
	}
	/******************* end moduleLoad **********************/
	/****************stock add click function *************/
	private void addStock() {
	    //抓下輸入的股票代號 並且轉成大寫
	    final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
	    newSymbolTextBox.setFocus(true);
	    Window.alert("Add Click! ----- symbol:"+symbol);

	    // Stock code must be between 1 and 10 chars that are numbers, letters, or dots.
	    if (!symbol.matches("^[0-9A-Z\\.]{1,10}$")) {
	      Window.alert("'" + symbol + "' is not a valid symbol.");
	      newSymbolTextBox.selectAll();
	      return;
	    }

	    newSymbolTextBox.setText("");

	    // Don't add the stock if it's already in the table.
	    if (stocks.contains(symbol))
	        return;

	    // Add the stock to the table.
	    int row = stocksFlexTable.getRowCount();
	    stocks.add(symbol);
	    stocksFlexTable.setText(row, 0, symbol);
	    // for dynamic dependent style
	    stocksFlexTable.setWidget(row, 2, new Label());
	    //只讓Price&Change 向右邊靠
	    stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
	    stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
	    //讓remove button 靠中間
	    stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");

	    // Add a button to remove this stock from the table.
	    Button removeStockButton = new Button("x");
	    // Add dependent styles => they are automatically updated whenever the primary style name changes.
	    removeStockButton.addStyleDependentName("remove");
	    
	    removeStockButton.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	      // stocks is a ArrayList . 
	      /*
		  * java.util.ArrayList的indexOf()，這個method是幫你找出你所傳入的object，
		  * 是否存在於ArrayList中，倘若有，則回傳它在ArrayList中的第幾個位置。
		  * */
	        int removedIndex = stocks.indexOf(symbol);
	      // 根據ArrayList的index來移除此object
	        stocks.remove(removedIndex);        
	      // 因為row的第一排是 Symbol Price Change Remove ; 所以從第二排開始
	        stocksFlexTable.removeRow(removedIndex + 1);
	      }
	    });
	    stocksFlexTable.setWidget(row, 3, removeStockButton);


	    // Get the stock price.
	    refreshWatchList();

	}
	private void refreshWatchList() {
		// for log refresh time
		rootLogger.log(Level.INFO, "refreshWatchList");	
		
		/****************** create random stockPrice(s) ******************/
		final double MAX_PRICE = 100.0; // $100.00
	    final double MAX_PRICE_CHANGE = 0.02; // +/- 2%
	    // 有Add幾個stock 就new多大的型別為StockPrice的Array
	    StockPrice[] prices = new StockPrice[stocks.size()];
	    for (int i = 0; i < stocks.size(); i++) {
	    // random 產生 price & change
	      double price = Random.nextDouble() * MAX_PRICE;
	      double change = price * MAX_PRICE_CHANGE * (Random.nextDouble() * 2.0 - 1.0);

	      prices[i] = new StockPrice(stocks.get(i), price, change);
	    }

	    updateTable(prices);
	}
	/**
	   * Update the Price and Change fields all the rows in the stock table.
	   *
	   * @param prices Stock data for all rows.
	   */
	private void updateTable(StockPrice[] prices) {
		// for log refresh time
	    rootLogger.log(Level.INFO, "updateTable");	
	    
	    for (int i = 0; i < prices.length; i++) {
	        updateTablePrice(prices[i]);
	      }
	 // Display timestamp showing last refresh.  refresh last update 時間
	 // replace getMediumDateTimeFormat
	 //lastUpdatedLabel.setText("Last update : "  + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));
	    lastUpdatedLabel.setText("Last update : "  + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM).format(new Date()));
	}
	/**
	   * Update a single row in the stock table.
	   *
	   * @param price Stock data for a single row.
	   */
	private void updateTablePrice(StockPrice price) {
	    // Make sure the stock is still in the stock table.
	    if (!stocks.contains(price.getSymbol())) {
	      return;
	    }
	 // 因為row的第一排是 Symbol Price Change Remove ; 所以從第二排開始
	    int row = stocks.indexOf(price.getSymbol()) + 1;

	    // 原本 price & change 都是double , 用NumberFormat 決定output的樣子 , 最後將Double並轉成String
	    // Format the data in the Price and Change fields.
	    String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
	    
	    NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
	    
	    String changeText = changeFormat.format(price.getChange());
	    String changePercentText = changeFormat.format(price.getChangePercent());

	    // Populate the Price and Change fields with new data.
	    stocksFlexTable.setText(row, 1, priceText);
	    
	    //stocksFlexTable.setText(row, 2, changeText + " (" + changePercentText+ "%)");
	    // for dynamic dependent style
	    Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
	    changeWidget.setText(changeText + " (" + changePercentText + "%)");
	    
	    // Change the color of text in the Change field based on its value.
	    String changeStyleName = "noChange";
	    if (price.getChangePercent() < -0.1f) {
	      changeStyleName = "negativeChange";
	    }
	    else if (price.getChangePercent() > 0.1f) {
	      changeStyleName = "positiveChange";
	    }

	    changeWidget.setStyleName(changeStyleName);
	}
	
}
