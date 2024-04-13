package routes

import akka.http.scaladsl.server.Directives._

object StaticRoute {
  val jsRoute =
    pathPrefix("js") {
      get {
        getFromResourceDirectory("public/js")
      }
    }
    
  val cssRoute =
    pathPrefix("css") {
      get {
        getFromResourceDirectory("public/css")
      }
    }  
    
    
}
