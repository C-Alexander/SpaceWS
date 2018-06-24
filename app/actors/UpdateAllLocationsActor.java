package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import play.Logger;

public class UpdateAllLocationsActor extends AbstractActor {

    public static Props props() {
        return Props.create(UpdateAllLocationsActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::handleMessage)
                .matchAny(message -> Logger.error("Unknown message: " + message))
                .build();
    }

    private void handleMessage(String message) {
        getSender().tell(message, getSelf());

        getContext().getSystem().actorOf(MoveShipsHTTPActor.props()).tell(message, self());
        Logger.info("Got a message: " + message);
    }
}