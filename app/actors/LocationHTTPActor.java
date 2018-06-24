package actors;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Logger;
import play.libs.Json;

public class LocationHTTPActor extends AbstractActor {
    final Http http = Http.get(context().system());

    public static Props props() {
        return Props.create(LocationHTTPActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::updateLocation)
                .build();
    }

    private void updateLocation(String location) {
        if (location.equals("update")) return;
        try {
            JsonNode parse = Json.parse(location);
            ObjectMapper objectMapper = new ObjectMapper();


            http.singleRequest(HttpRequest.PATCH("http://localhost:3000/ships/" + parse.get("id").asText())
                    .withEntity(
                            HttpEntities.create(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsString(parse.get("destination")))
                    ));
        } catch (Exception e) {
            Logger.error(e.getMessage(), e);
        }
        self().tell(PoisonPill.getInstance(), self());
    }
}
