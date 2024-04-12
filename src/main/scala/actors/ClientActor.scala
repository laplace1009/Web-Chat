package actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import io.circe.syntax._
import io.circe.generic.auto._
import routes.ChatMessage

object ClientActor {
  sealed trait Command
  case class SendMessage(message: String) extends Command
  case class RecvMessage(message: String) extends Command
  case class SetUserName(userName: String) extends Command
  case object NotBehavior extends Command
  case object Disconnect extends Command
  case object ActorStopped extends Command

  def apply(initialUserName: String)(implicit actorSystem: ActorSystem[ServerActor.Command]): Behavior[Command] = {
    Behaviors.setup { context =>
      def behavior(userName: String): Behavior[Command] = Behaviors.receiveMessage {
        case SendMessage(message) =>
          Behaviors.same
        case RecvMessage(message) =>
          actorSystem ! ServerActor.Broadcast(message)
          Behaviors.same
        case SetUserName(newUserName) =>
          val newClient = ChatMessage(newUserName, s"/n $userName -> $newUserName")
          val json = newClient.asJson.noSpaces
          actorSystem ! ServerActor.ChangeUserName(context.self, json)
          behavior(newUserName)
        case ActorStopped =>
          println("Stopped Actor")
          Behaviors.stopped
        case Disconnect =>
          actorSystem ! ServerActor.Disconnect(context.self)
          Behaviors.same
        case NotBehavior =>
          Behaviors.same
      }
      behavior(initialUserName)
    }
  }
}

