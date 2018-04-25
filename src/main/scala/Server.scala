package jsonvalidator

import java.io.{InputStream, OutputStream}
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.json4s.{JObject, JValue, JString}
import org.json4s.native.JsonMethods

object Server {
  def start(port : Int, templateStore : TemplateStore) {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", new RequestHandler(templateStore))
    server.setExecutor(null)

    server.start()
  }
}

class RequestHandler(templateStore : TemplateStore) extends HttpHandler {
  def handle(req : HttpExchange) {
    val method = req.getRequestMethod
    val path   = req.getRequestURI.getPath

    val pathFormat = """/([a-z]{1,})/([A-Za-z0-9]{1,})""".r
    (method, path) match {
      case ("GET",  pathFormat("schema",   schemaId)) => handleGetSchema(req, schemaId)
      case ("POST", pathFormat("schema",   schemaId)) => handlePostSchema(req, schemaId)
      case ("POST", pathFormat("validate", schemaId)) => handleValidateSchema(req, schemaId)
      case default =>
        sendJsonResponse(req, 400, JObject(List(
          ("action",  JString("invalid")),
          ("status",  JString("error")),
          ("method",  JString(method)),
          ("path",    JString(path)),
          ("message", JString("Bad path or method"))
        )))
    }
  }

  def handleGetSchema(req : HttpExchange, schemaId : String) {
    templateStore.getTemplateSource(schemaId) match {
      case Some(schemaSource) =>
        sendStringResponse(req, 200, schemaSource.mkString)
      case None =>
        sendJsonResponse(req, 404, JObject(List(
          ("action",  JString("getSchema")),
          ("id",  JString(schemaId)),
          ("status",  JString("fail")),
          ("message", JString("Schema Not Found"))
        )))
    }
  }

  def handlePostSchema(req : HttpExchange, schemaId : String) {
    sendJsonResponse(req, 400, JObject(List(
      ("action",  JString("uploadSchema")),
      ("id",  JString(schemaId)),
      ("status",  JString("fail"))
    )))
  }

  def handleValidateSchema(req : HttpExchange, schemaId : String) {
    sendJsonResponse(req, 400, JObject(List(
      ("action",  JString("validateSchema")),
      ("id",  JString(schemaId)),
      ("status",  JString("fail"))
    )))
  }

  private def sendJsonResponse(req : HttpExchange, status : Int, json : JValue) {
    val resText = JsonMethods.pretty( JsonMethods.render(json) )
    sendStringResponse(req, status, resText)
  }

  private def sendStringResponse(req : HttpExchange, status : Int, resText : String) {
    req.sendResponseHeaders(status, resText.length)
    req.getResponseBody.write(resText.getBytes)
    req.getResponseBody.close()
  }
}
