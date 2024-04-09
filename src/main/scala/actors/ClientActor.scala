package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ClientActor {
  sealed trait Command
  case object Disconnect extends Command
  case class SendMessage(message: String) extends Command
  case class RecvMessage(message: String) extends Command

  def apply(server: ActorRef[ServerActor.Command]): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case SendMessage(message) =>
        context.log.info(s"Sending message: $message")
        server ! ServerActor.Broadcast(message)
        Behaviors.same
      case RecvMessage(message) =>
        context.log.info(s"Recving message: $message")
        Behaviors.same
      case Disconnect =>
        context.log.info(s"Disconnect Server")
        Behaviors.stopped
      case _ =>
        context.log.warn(s"Don't reach this case")
        Behaviors.unhandled
    }
  }
}

final case class ClientSession(nickName: String, clientActor: ActorRef[ClientActor.Command])