# TradingApp

This is a Java application that connects to the WazirX WebSocket API to fetch real-time
trading data and prepares payloads for order operations based on a specified trigger price input by
the user.
The program uses the Java WebSocket API to establish a connection and handle messages from the WebSocket server. 

1. Package and Imports:

```
package com.websocket_wazirx.trading_app;
import javax.websocket.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.Scanner;
```

The package com.websocket_wazirx.trading_app organizes the code.
Necessary imports are included:
javax.websocket.* for WebSocket support.
org.json.* for JSON parsing.
org.slf4j.* for logging.
java.net.URI for WebSocket URI.
java.util.Scanner for user input.


2. Class Definition and Variables:
@ClientEndpoint
```
public class WazirXWebSocketClient {
    private static final String API_URL = "wss://stream.wazirx.com/stream";
    private static double triggerPrice;
    private static boolean isBuyOrderPlaced = false;
    private static boolean isSellOrderPlaced = false;
    private static final Logger logger = LoggerFactory.getLogger(WazirXWebSocketClient.class);
    private static Session userSession = null;
}
```
The class WazirXWebSocketClient is annotated with @ClientEndpoint to indicate it is a WebSocket client endpoint.
The static variables include:
API_URL for the WebSocket API endpoint.
triggerPrice to store the user-specified price.
Flags isBuyOrderPlaced and isSellOrderPlaced to track order placement.
A logger for logging messages.
userSession to manage the WebSocket session.


3. WebSocket Event Handlers:
onOpen Method:
```
@OnOpen
public void onOpen(Session session) {
    logger.info("Connected to WazirX WebSocket");
    userSession = session;
    session.getAsyncRemote().sendText("{\"event\":\"subscribe\",\"streams\":[\"btcinr@trade\"]}");
}
```
Called when the WebSocket connection is opened.
Logs the connection.
Subscribes to the btcinr@trade stream for real-time trading data.

onMessage Method:
```
@OnMessage
public void onMessage(String message) {
    logger.info("Received: " + message);
    try {
        double marketPrice = extractPriceFromMessage(message);
        processMarketData(marketPrice);
    } catch (JSONException e) {
        logger.warn("Received message without price: " + message);
    }
}
```
Called when a message is received.
Logs the received message.
Extracts the price from the message and processes the market data.

onError Method:
```
@OnError
public void onError(Session session, Throwable throwable) {
    logger.error("Error: ", throwable);
}
```
Called when an error occurs.
Logs the error.

onClose Method:
```
@OnClose
public void onClose(Session session, CloseReason closeReason) {
    logger.info("Connection closed: " + closeReason.getReasonPhrase());
}
```
Called when the WebSocket connection is closed.
Logs the reason for closing.


4. Helper Methods
extractPriceFromMessage Method:
```
private double extractPriceFromMessage(String message) {
    JSONObject jsonObject = new JSONObject(message);
    if (!jsonObject.has("data") || !jsonObject.getJSONObject("data").has("price")) {
        throw new JSONException("No price field found in the message");
    }
    String priceString = jsonObject.getJSONObject("data").getString("price");
    return Double.parseDouble(priceString);
}
```
Parses the JSON message to extract the price.
Throws an exception if the price is not found.

processMarketData Method:
```
private void processMarketData(double marketPrice) {
    if (!isBuyOrderPlaced && marketPrice <= triggerPrice) {
        String buyOrderPayload = prepareBuyOrderPayload(marketPrice);
        System.out.println("Prepared payload for buy order: " + buyOrderPayload);
        isBuyOrderPlaced = true;
    } else if (!isSellOrderPlaced && marketPrice >= triggerPrice) {
        String sellOrderPayload = prepareSellOrderPayload(marketPrice);
        System.out.println("Prepared payload for sell order: " + sellOrderPayload);
        isSellOrderPlaced = true;
    }
}
```
Processes market data to determine if a buy or sell order should be placed based on the trigger price.
Prepares the respective order payload and marks the order as placed.

prepareBuyOrderPayload and prepareSellOrderPayload Methods:
```
private String prepareBuyOrderPayload(double price) {
    return String.format("{\"type\":\"buy\",\"price\":%.2f}", price);
}

private String prepareSellOrderPayload(double price) {
    return String.format("{\"type\":\"sell\",\"price\":%.2f}", price);
}
```
Formats JSON payloads for buy and sell orders with the given price.

prepareCancelOrderPayload Method:
```
private String prepareCancelOrderPayload(String orderId) {
    return String.format("{\"type\":\"cancel\",\"orderId\":\"%s\"}", orderId);
}
```
Formats JSON payload for canceling an order with the given order ID.


5. Main Method:
```
public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Enter trigger price:");
    triggerPrice = scanner.nextDouble();
    scanner.nextLine(); 

    System.out.println("Connecting to WazirX WebSocket...");
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    try {
        container.connectToServer(WazirXWebSocketClient.class, new URI(API_URL));
    } catch (Exception e) {
        logger.error("Connection error: ", e);
    }

    while (true) {
        System.out.println("Enter command (quit to exit):");
        String command = scanner.nextLine();
        if ("quit".equalsIgnoreCase(command)) {
            System.out.println("Exiting...");
            break;
        }
    }
    try {
        if (userSession != null) {
            userSession.close();
        }
    } catch (Exception e) {
        logger.error("Error closing WebSocket session: ", e);
    }
}
```
Prompts the user to enter a trigger price.
Connects to the WazirX WebSocket API.
Listens for user commands, allowing the user to quit the application.
Closes the WebSocket session when exiting.
