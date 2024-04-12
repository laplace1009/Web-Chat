package routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
object ChatRoute {
  def topLevelRoute(implicit system: ActorSystem[_]): Route = {
    pathEndOrSingleSlash {
      get {
        getFromResource("public/html/index.html")
      }
    }
  }
}
