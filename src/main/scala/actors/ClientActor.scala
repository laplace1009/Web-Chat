package actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ClientActor {
  sealed trait Command
  case class SendMessage(message: String, userName: String) extends Command
  case class RecvMessage(message: String) extends Command
  case object Disconnect extends Command

  def apply(userName: String)(implicit actorSystem: ActorSystem[ServerActor.Command]): Behavior[Command] = Behaviors.setup {
    context =>
      Behaviors.receiveMessage {
        case SendMessage(message, userName) =>
          println(s"Client Actor SendMsg: $message")
          Behaviors.same
        case RecvMessage(message) =>
          println(s"Client Actor RecvMsg: $message")
          actorSystem ! ServerActor.Broadcast(message)
          Behaviors.same
        case Disconnect =>
          println("Disconnect ClientActor")
          Behaviors.stopped
      }
  }
}

