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
import io.circe.parser._
import io.circe.generic.auto._

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

case class ChatMessage(userName: String, message: String)

def webSocketFlow(implicit system: ActorSystem[ServerActor.Command]): (ActorRef[ClientActor.Command], Flow[Message, Message, _]) = {
  val userName = Random.nextLong().toString
  val clientActor: ActorRef[ClientActor.Command] = system.systemActorOf(ClientActor(userName), userName + "clientActor")
  val incomingMessage: Sink[Message, _] = Flow[Message].collect {
    case TextMessage.Strict(text: String) =>
      decode[ChatMessage](text).toOption.flatMap {
        case ChatMessage(_, message) if message.startsWith("/n ") =>
          val arr = message.split(" ")
          if (arr.length == 2) {
            Some(ClientActor.SetUserName(arr(1)))
          } else {
            Some(ClientActor.NotBehavior)
          }
        case ChatMessage(userName, message) =>
          Some(ClientActor.RecvMessage(text))
        case _ =>
          None
      }
  }.collect {
    case Some(command) => command
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
    case ClientActor.SendMessage(message) =>
      println(message)
      TextMessage(message)
  }

  val flow = Flow.fromSinkAndSourceCoupled(incomingMessage, outgoingMessage)
  (clientActor, flow)
}