import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.io.StdIn
import scala.util.{Failure, Success}

import akka.util.Timeout
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.pattern.after
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import routes.{ChatRoute, StaticRoute}
import actors.{GuardianActor, ServerActor}



@main
def main(): Unit = {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(GuardianActor(), "ChatServer")
  implicit val executionContext = system.executionContext
  val timeout = Timeout(5.seconds)
  val serverServiceKey = ServerActor.ServerServiceKey
  val serverActorFuture = system.receptionist.ask[Receptionist.Listing](ref => Receptionist.Find(serverServiceKey, ref))(timeout, system.scheduler)

  def retryLookup(delay: FiniteDuration, attempts: Int): Future[Option[ActorRef[ServerActor.Command]]] = {
    after(delay, using = system.scheduler.toClassic) {
      system.receptionist.ask[Receptionist.Listing](ref => Receptionist.Find(serverServiceKey, ref))(timeout, system.scheduler).map { listing =>
        listing.serviceInstances(serverServiceKey).headOption
      }
    }.flatMap {
      case Some(actorRef) => Future.successful(Some(actorRef))
      case None if attempts > 1 => retryLookup(delay, attempts - 1)
      case None => Future.successful(None)
    }
  }

  retryLookup(1.seconds, 5).onComplete {
    case Success(Some(serverRef)) =>
      val routes = StaticRoute.jsRoute ~ ChatRoute.topLevelRoute(serverRef)
      Http()(system).newServerAt("localhost", 8080).bind(routes).onComplete {
        case Success(_) =>
          println(s"Server now online. Please navigate to http://localhost:8080/\nPress RETURN to stop...")
          StdIn.readLine()
          system.terminate()
        case Failure(exception) =>
          println(s"Failed to bind HTTP server: ${exception.getMessage}")
          system.terminate()
      }
    case Success(None) => println("Failed to find ServerActor after retries.")
    case Failure(exception) => println(s"Error during lookup: ${exception.getMessage}")
  }
}