package routes

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.OverflowStrategy
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import actors.{ClientActor, ServerActor}
import scala.util.Random
import io.circe.parser._
import io.circe.syntax._
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
case class ChatMessage(changeUser: Boolean, changeList: Boolean, userName: String, message: String, userList: Array[String])
def webSocketFlow(implicit system: ActorSystem[ServerActor.Command]): (ActorRef[ClientActor.Command], Flow[Message, Message, _]) = {
  val limitRandomNumber = 100000000
  val userName = "user" + Random.nextLong(limitRandomNumber).toString
  val clientActor: ActorRef[ClientActor.Command] = system.systemActorOf(ClientActor(userName), userName + "clientActor")
  val incomingMessage: Sink[Message, _] = Flow[Message].collect {
    case TextMessage.Strict(text: String) =>
      decode[ChatMessage](text).toOption.flatMap {
        case ChatMessage(true, _, _, message, _) =>
          Some(ClientActor.SetUserName(message))
        case ChatMessage(_, _, _, _, _) =>
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
    val json = ChatMessage(true, false, "", userName, Array()).asJson.noSpaces
    system ! ServerActor.Connect(userName, (clientActor, outgoingActor))
    outgoingActor ! ClientActor.SetUserName(json)
    outgoingActor
  }.collect {
    case ClientActor.SendMessage(message) =>
      TextMessage(message)
    case ClientActor.SetUserName(message) =>
      TextMessage(message)
  }

  val flow = Flow.fromSinkAndSourceCoupled(incomingMessage, outgoingMessage)
  (clientActor, flow)
}