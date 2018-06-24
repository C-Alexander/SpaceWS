package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.google.inject.Injector;
import play.Logger;

public class UpdateLocationActor extends AbstractActor {


    private Injector injector;

    public UpdateLocationActor(Injector injector) {

        this.injector = injector;
    }

    public static Props props(Injector injector) {
        return Props.create(UpdateLocationActor.class, injector);
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

        getContext().getSystem().actorOf(LocationHTTPActor.props()).tell(message, self());
        Logger.info("Got a message: " + message);
    }
}