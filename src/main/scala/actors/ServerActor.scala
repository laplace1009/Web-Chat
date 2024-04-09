package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}

object ServerActor {
  sealed trait Command
  case object Connect extends Command
  case class Disconnect(clientId: Int) extends Command
  case class Broadcast(message: String) extends Command
  val ServerServiceKey: ServiceKey[Command] = ServiceKey[Command]("SERVER_KEY")

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(ServerServiceKey, context.self)
    context.log.info(s"Registering ServerActor with ${ServerServiceKey.id}")
    def updatedBehavior(clients: Map[Int, ClientSession], clientCount: Int): Behavior[Command] =
      Behaviors.receiveMessage {
        case Connect =>
          val newClientCount = clientCount + 1
          val clientNickname = s"user$newClientCount"
          val clientActor = context.spawn(ClientActor(context.self), s"$newClientCount")
          val clientSession = ClientSession(clientNickname, clientActor)
          val updatedClients = clients + (newClientCount -> clientSession)
          updatedBehavior(updatedClients, newClientCount)

        case Disconnect(clientId) =>
          val updatedClients = clients.removed(clientId)
          updatedBehavior(updatedClients, clientCount)

        case Broadcast(message) =>
          clients.values.foreach {clientSession =>
            clientSession.clientActor ! ClientActor.RecvMessage(message)
          }
          Behaviors.same

        case _ =>
          context.log.info("Don't reach this case")
          Behaviors.same
      }

    updatedBehavior(Map.empty[Int, ClientSession], 0)
  }
}
