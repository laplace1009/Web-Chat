package routes

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.{OverflowStrategy}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import actors.{ClientActor, ServerActor}

import scala.util.Random

//def webSocketRoute(implicit system: ActorSystem[ServerActor.Command]): Route = {
//  path("ws") {
//    handleWebSocketMessages(chatRoomFlow)
//  }
//}
//
//def chatRoomFlow(implicit system: ActorSystem[ServerActor.Command]): Flow[Message, Message, Any] = {
//  val serverActorRef = system
//
//  Flow[Message]
//    .collect {
//      case TextMessage.Strict(text) => text
//    }
//    .mapAsync(1) { text =>
//      // Pretend processing logic; in reality, you might want to handle this text
//      // For example, broadcasting to all clients via the server actor
//      system ! ServerActor.Broadcast(text)
//      // Create a future that simply returns the text wrapped in a TextMessage
//      scala.concurrent.Future.successful(TextMessage(text))
//    }
//}


object WebSocketRoute {
  def webSocketRoute(implicit system: ActorSystem[ServerActor.Command]): Route = {
    concat {
      path("ws") {
        val (_, flow) = webSocketFlow
        handleWebSocketMessages(flow)
      }
    }
  }
}

def webSocketFlow(implicit system: ActorSystem[ServerActor.Command]): (ActorRef[ClientActor.Command], Flow[Message, Message, _]) = {
  val userName = Random.nextLong().toString
  val clientActor: ActorRef[ClientActor.Command] = system.systemActorOf(ClientActor(userName), userName + "clientActor")
  val incomingMessage: Sink[Message, _] = Flow[Message].collect {
    case TextMessage.Strict(text: String) =>
      println(s"Client SendMessage: $text")
      ClientActor.RecvMessage(text)
  }.to(ActorSink.actorRef[ClientActor.Command](
    ref = clientActor,
    onCompleteMessage = ClientActor.Disconnect,
    onFailureMessage = throwable => ClientActor.Disconnect,
  ))
  val outgoingMessage: Source[Message, _] = ActorSource.actorRef[ClientActor.Command](
    completionMatcher = PartialFunction.empty,
    failureMatcher = PartialFunction.empty,
    bufferSize = 10,
    overflowStrategy = OverflowStrategy.dropHead
  ).mapMaterializedValue { outgoingActor =>
    system ! ServerActor.Connect((clientActor, outgoingActor))
    outgoingActor
  }.collect {
    case ClientActor.SendMessage(message, userName) =>
      TextMessage(message)
  }

  val flow = Flow.fromSinkAndSourceCoupled(incomingMessage, outgoingMessage)
  (clientActor, flow)
}
