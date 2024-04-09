package actors

import akka.actor.typed.{ActorSystem, SpawnProtocol, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}

object GuardianActor {
  sealed trait Command
  case object Connect extends Command
// case class Disconnect(String: string) extends Command
  case class RecvMessage(message: String) extends Command

  def apply(): Behavior[SpawnProtocol.Command] = {
    Behaviors.setup { context =>
      val server = context.spawn(ServerActor(), "server")
      
      Behaviors.receiveMessage {
        case Connect =>
          server ! ServerActor.Connect
          Behaviors.same
        case RecvMessage(message) =>
          server ! ServerActor.Broadcast(message)
          Behaviors.same
      }
    }
  }
}
