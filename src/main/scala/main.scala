import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.io.StdIn
import akka.util.Timeout
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives.*
import routes.{ChatRoute, StaticRoute, WebSocketRoute}
import actors.ServerActor
import akka.http.scaladsl.Http

@main
def main(): Unit = {
  implicit val system: ActorSystem[ServerActor.Command] = ActorSystem(ServerActor(), "ChatServer")
  implicit val executionContext = system.executionContext
  val timeout = Timeout(5.seconds)
  val routes = StaticRoute.jsRoute ~ ChatRoute.topLevelRoute ~ WebSocketRoute.webSocketRoute

  val serverFuture = Http().newServerAt("localhost", 8080).bind(routes)
  println(s"Server now online. Please navigate to http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  serverFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}