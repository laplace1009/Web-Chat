package routes

import akka.http.scaladsl.server.Directives._

object StaticRoute {
  lazy val jsRoute =
    pathPrefix("js") {
      get {
        getFromResourceDirectory("public/js")
      }
    }
}
