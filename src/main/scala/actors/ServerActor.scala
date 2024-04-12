package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors


object ServerActor {
  sealed trait Command
  case class Connect(clientActors: (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])) extends Command
  case class Disconnect(clientActor: ActorRef[ClientActor.Command]) extends Command
  case class ChangeUserName(clientActor: ActorRef[ClientActor.Command], message: String) extends Command
  case class Broadcast(message: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var clients: Map[String, (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])] = Map.empty
    var userId = 0
    Behaviors.receiveMessage {
      case Connect(clientActors) =>
        clients = clients + (userId.toString -> clientActors)
        userId = userId + 1
        Behaviors.same
      case Disconnect(clientActor) =>
        clients.collectFirst {
          case (id, (actor1, actor2)) if actor1 == clientActor || actor2 == clientActor =>
            actor1 ! ClientActor.ActorStopped
            actor2 ! ClientActor.ActorStopped
            clients = clients - id
        }
        context.log.info("Disconnect Successful")
        Behaviors.same
      case ChangeUserName(clientActor, message) => 
        clients.collectFirst {
          case (id, (recvActor, sendActor)) if recvActor == clientActor || sendActor == clientActor =>
            sendActor ! ClientActor.SendMessage(message)
        }
        Behaviors.same
      case Broadcast(message) =>
          clients.values.foreach { (_, send) =>
            send ! ClientActor.SendMessage(message)
          }
          Behaviors.same
      }
  }

}


