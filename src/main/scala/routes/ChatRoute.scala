package routes

import actors.ServerActor
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route

object ChatRoute {
  def topLevelRoute(serverActor: ActorRef[ServerActor.Command])(implicit system: ActorSystem[_]): Route = {

    concat(
      pathEndOrSingleSlash {
        get {
          getFromResource("public/html/index.html")
        }
      },
      path("ws") {
        handleWebSocketMessages(webSocketFlow(serverActor))
      }
    )
  }

  private def webSocketFlow(serverActor: ActorRef[ServerActor.Command]): Flow[Message, Message, _] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(text) => ServerActor.Broadcast(text)
      }
      .map { command =>
        serverActor ! command
        TextMessage("Message received")
      }
  }
}