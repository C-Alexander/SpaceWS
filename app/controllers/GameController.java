package controllers;

import actors.UpdateAllLocationsActor;
import actors.UpdateLocationActor;
import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.japi.pf.PFBuilder;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import akka.util.Timeout;
import com.google.inject.Injector;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.WebSocket;
import scala.concurrent.duration.FiniteDuration;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GameController extends Controller {

    private final Flow<String, String, NotUsed> gameFlow;
    private final Injector injector;


    @Inject
    public GameController(ActorSystem actorSystem,
                          Materializer mat, Injector injector) {
        this.injector = injector;
        ActorRef updateLocationActor = actorSystem.actorOf(UpdateLocationActor.props(injector));
        ActorRef updateAllLocationsActor = actorSystem.actorOf(UpdateAllLocationsActor.props());

        Timeout timeout = Timeout.apply(5, TimeUnit.SECONDS);

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
        LoggingAdapter logging = Logging.getLogger(actorSystem.eventStream(), logger.getName());

        //noinspection unchecked
        Source<String, Sink<String, NotUsed>> source = MergeHub.of(String.class)
                .log("source", logging)
                .ask(5, updateLocationActor, String.class, timeout)
                .recoverWithRetries(-1, new PFBuilder().match(Throwable.class, e -> Source.empty()).build());

        Sink<String, Source<String, NotUsed>> sink = BroadcastHub.of(String.class);

        Pair<Sink<String, NotUsed>, Source<String, NotUsed>> sinkSourcePair = source.toMat(sink, Keep.both()).run(mat);
        Sink<String, NotUsed> movementSink = sinkSourcePair.first();
        Source<String, NotUsed> movementSource = sinkSourcePair.second();

        this.gameFlow = Flow.fromSinkAndSource(movementSink, movementSource).log("gameFlow", logging);

        Source.tick(FiniteDuration.Zero(), FiniteDuration.create(10, "s"), "update")
                .ask(1, updateAllLocationsActor, String.class, timeout)
                .runWith(movementSink, mat);
    }

    public WebSocket game() {
        return WebSocket.Text.acceptOrResult(request -> CompletableFuture.completedFuture(F.Either.Right(gameFlow)));
    }

}
