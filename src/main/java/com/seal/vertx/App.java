package com.seal.vertx;

import com.seal.vertx.domain.GameState;
import com.seal.vertx.message.ActionMessage;
import com.seal.vertx.message.ActionMessageCodec;
import com.seal.vertx.message.GameStateCodec;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        addCodecs(vertx);
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route("/client/*").handler(StaticHandler.create("client"));

        // pipe ws <-> vertx bux
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions bridgeOptions = new BridgeOptions();
        PermittedOptions permitAll = new PermittedOptions();
        permitAll.setAddressRegex(".*");
        bridgeOptions.addInboundPermitted(permitAll);
        bridgeOptions.addOutboundPermitted(permitAll);
        SockJSHandler bridge = sockJSHandler.bridge(bridgeOptions);

        router.route("/eventbus/*").handler(bridge);

        router.route().handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("catchall");
        });

        vertx.eventBus().consumer("draw", msg -> {
           LOG.info("Got: {}", msg.body());
        });

        server.requestHandler(router::accept).listen(8080);

    }

    private static void addCodecs(Vertx vertx) {
        vertx.eventBus().registerDefaultCodec(ActionMessage.class, new ActionMessageCodec());
        vertx.eventBus().registerDefaultCodec(GameState.class, new GameStateCodec());
    }

}
