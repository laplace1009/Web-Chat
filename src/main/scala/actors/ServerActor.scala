package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors}


object ServerActor {
  sealed trait Command
  case class Connect(clientActors: (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])) extends Command
  case class Disconnect(clientId: String) extends Command
  case class Broadcast(message: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var clients: Map[String, (ActorRef[ClientActor.Command], ActorRef[ClientActor.Command])] = Map.empty
    var userId = 0
    Behaviors.receiveMessage {
      case Connect(clientActors) =>
        clients = clients + (userId.toString() -> clientActors)
        userId += 1
        println("New ClientActor created.")
        Behaviors.same
      case Disconnect(clientId) =>
//        clients.get(clientId).foreach(context.stop)
//        clients = clients - clientId
        println(s"ClientActor with ID $clientId removed")
        Behaviors.same
      case Broadcast(message) =>
          println(s"Server broadcast message: $message")
          clients.values.foreach { (recv, send) =>
            send ! ClientActor.SendMessage(message, "user1")
          }
          Behaviors.same
      }
  }

}


