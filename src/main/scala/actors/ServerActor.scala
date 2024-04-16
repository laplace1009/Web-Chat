package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import io.circe.syntax.*
import io.circe.generic.auto.*
import routes.ChatMessage


object ServerActor {
  sealed trait Command
  case class Connect(userName: String, clientActors: (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])) extends Command
  case class Disconnect(clientActor: ActorRef[ClientActor.Command]) extends Command
  case class ChangeUserName(prevUserName: String, newUserName: String, clientActor: ActorRef[ClientActor.Command], message: ChatMessage) extends Command
  case object GetUserList extends Command
  case class Broadcast(message: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var clients: Map[String, (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])] = Map.empty
    Behaviors.receiveMessage {
      case Connect(userName, clientActors) =>
        clients = clients + (userName -> clientActors)
        context.self ! ServerActor.GetUserList
        Behaviors.same
      case Disconnect(clientActor) =>
        clients.collectFirst {
          case (id, (actor1, actor2)) if actor1 == clientActor || actor2 == clientActor =>
            actor1 ! ClientActor.ActorStopped
            actor2 ! ClientActor.ActorStopped
            clients = clients - id
        }
        context.log.info("Disconnect Successful")
        context.self ! ServerActor.GetUserList
        Behaviors.same
      case ChangeUserName(prevUserName, newUserName, clientActor, chatMessage: ChatMessage) =>
        val copyClient = clients(prevUserName)
        clients -= prevUserName
        clients = clients + (newUserName -> copyClient)
        val newClientMessage: ChatMessage = ChatMessage(true, false, prevUserName, chatMessage.message, Array())
        val clientJson = newClientMessage.asJson.noSpaces
        copyClient._2 ! ClientActor.SendMessage(clientJson)
        context.self ! ServerActor.GetUserList
        Behaviors.same
      case GetUserList =>
        val userNameList = clients.map { (userName, _) =>
          userName
        }.toArray
        val newBroadcastMessage: ChatMessage = ChatMessage(false, true, "server", "", userNameList)
        val broadcastJson = newBroadcastMessage.asJson.noSpaces
        context.self ! ServerActor.Broadcast(broadcastJson)
        Behaviors.same
      case Broadcast(message) =>
          clients.values.foreach { (_, send) =>
            send ! ClientActor.SendMessage(message)
          }
          Behaviors.same
      }
  }
}


