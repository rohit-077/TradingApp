package com.websocket_wazirx.trading_app;

import javax.websocket.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class WazirXWebSocketClient {
    private static final String API_URL = "wss://stream.wazirx.com/stream";
    private static double triggerPrice;
    private static boolean isBuyOrderPlaced = false;
    private static boolean isSellOrderPlaced = false;
    private static final Logger logger = LoggerFactory.getLogger(WazirXWebSocketClient.class);
    private static Session userSession = null;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected to WazirX WebSocket");
        userSession = session;
        session.getAsyncRemote().sendText("{\"event\":\"subscribe\",\"streams\":[\"btcinr@trade\"]}");
    }

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


    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Error: ", throwable);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Connection closed: " + closeReason.getReasonPhrase());
    }

    private double extractPriceFromMessage(String message) {
        JSONObject jsonObject = new JSONObject(message);

        if (!jsonObject.has("data") || !jsonObject.getJSONObject("data").has("price")) {
            throw new JSONException("No price field found in the message");
        }

        String priceString = jsonObject.getJSONObject("data").getString("price");
        return Double.parseDouble(priceString);
    }


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


    private String prepareBuyOrderPayload(double price) {
        return String.format("{\"type\":\"buy\",\"price\":%.2f}", price);
    }

    private String prepareSellOrderPayload(double price) {
        return String.format("{\"type\":\"sell\",\"price\":%.2f}", price);
    }

    private String prepareCancelOrderPayload(String orderId) {
        return String.format("{\"type\":\"cancel\",\"orderId\":\"%s\"}", orderId);
    }

    
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
}
