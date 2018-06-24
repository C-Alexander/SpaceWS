package controllers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Logger;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.ws.WebSocket;
import play.shaded.ahc.org.asynchttpclient.ws.WebSocketListener;
import play.test.WithServer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class GameControllerTest extends WithServer {

    private AsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() {
        asyncHttpClient = new DefaultAsyncHttpClient();
    }

    @After
    public void tearDown() throws IOException {
        asyncHttpClient.close();
    }

    @Test
    public void testWebsocket() throws Exception {
        String serverURL = "ws://localhost:" + this.testServer.port() + "/game";

        WebSocketClient webSocketClient = new WebSocketClient(asyncHttpClient);
        CompletableFuture<WebSocket> future = webSocketClient.call(serverURL, serverURL, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                Logger.debug("Opened websocket" + webSocket.getRemoteAddress());
            }

            @Override
            public void onClose(WebSocket webSocket) {
                Logger.debug("Closed websocket" + webSocket.getRemoteAddress());
            }

            @Override
            public void onError(Throwable throwable) {
                Logger.error("Error in websocket", throwable);

            }
        });
        await().untilAsserted(() -> assertThat(future).isDone());
        assertThat(future).isCompletedWithValueMatching(WebSocket::isOpen);
    }


}