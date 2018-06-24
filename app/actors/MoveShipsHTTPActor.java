package actors;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Json;

public class MoveShipsHTTPActor extends AbstractActor {
    private final Http http = Http.get(context().system());
    private final float alpha = 0.1f;
    private final float inversedAlpha = (1.0f - alpha);

    public static Props props() {
        return Props.create(MoveShipsHTTPActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::updateLocation)
                .build();
    }

    private void updateLocation(String message) {
        if (!message.equals("update")) return;
        ActorMaterializer actorMaterializer = ActorMaterializer.create(getContext());

        try {
            HttpResponse httpResponse = http.singleRequest(HttpRequest.GET("http://localhost:3000/ships"))
                    .toCompletableFuture().get();

            if (httpResponse.status().intValue() != 200)
                Logger.error("Problem whilst getting all ships");

            handleHTTPResponse(actorMaterializer, httpResponse);

        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
        }
        self().tell(PoisonPill.getInstance(), self());
    }

    private void handleHTTPResponse(ActorMaterializer actorMaterializer, HttpResponse httpResponse) {
        httpResponse.entity().toStrict(10000, actorMaterializer)
                .thenApply(entity ->
                        entity.getData().utf8String())
                .thenApply(body -> {

                    JsonNode parsedBody = Json.parse(body);
                    for (JsonNode jsonNode : parsedBody) {
                        String json = getJsonFromResponseNode(jsonNode);

                        if (json == null) break;

                        try {
                            http.singleRequest(HttpRequest.PATCH("http://localhost:3000/ships/" + jsonNode.get("id").asText())
                                    .withEntity(
                                            HttpEntities.create(ContentTypes.APPLICATION_JSON, json)
                                    ));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return "";
                });
    }

    private String getJsonFromResponseNode(JsonNode jsonNode) {
        String id = jsonNode.get("id").asText();
        int yPos = jsonNode.get("yPos").asInt();
        int xPos = jsonNode.get("xPos").asInt();
        int xDestination = jsonNode.get("xDestination").asInt();
        int yDestination = jsonNode.get("yDestination").asInt();

        if (xPos == xDestination && yPos == yDestination) return null;

        xPos = (int) (((float) xPos * inversedAlpha) + ((float) xDestination * alpha));
        yPos = (int) (((float) yPos * inversedAlpha) + ((float) yDestination * alpha));

        String json = "{ \"xDest\":\"" + xPos + "\", \"yDest\":\"" + yPos + "\" }";

        return json;
    }
}
