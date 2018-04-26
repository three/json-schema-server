package jsonvalidator

import java.io.{InputStream, OutputStream, File}
import java.net.InetSocketAddress
import java.nio.file.Files
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import scala.io.Source
import org.json4s.{JObject, JValue, JString}
import org.json4s.jackson.JsonMethods

object Server {
  def start(port : Int, templateStore : TemplateStore) {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", new RequestHandler(templateStore))
    server.setExecutor(null)

    server.start()
  }
}

/**
 * Handles all requests made to the server
 */
class RequestHandler(templateStore : TemplateStore) extends HttpHandler {
  def handle(req : HttpExchange) {
    val method = req.getRequestMethod
    val path   = req.getRequestURI.getPath

    println(method + " " + path)

    // Will match URLs of the form /abc/def
    val pathFormat = """/([a-z]{1,})/([A-Za-z0-9_-]{1,})""".r
    (method, path) match {
      case ("GET",  pathFormat("schema",   schemaId)) =>
        handleGetSchema(req, schemaId)
      case ("POST", pathFormat("schema",   schemaId)) =>
        handlePostSchema(req, schemaId)
      case ("POST", pathFormat("validate", schemaId)) =>
        handleValidateDocument(req, schemaId)
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
    templateStore.getSchemaSource(schemaId) match {
      case Some(schemaSource) =>
        sendStringResponse(req, 200, schemaSource.mkString)
      case None =>
        sendJsonResponse(req, 404, JObject(List(
          ("action",  JString("getSchema")),
          ("id",      JString(schemaId)),
          ("status",  JString("error")),
          ("message", JString("Schema Not Found"))
        )))
    }
  }

  def handlePostSchema(req : HttpExchange, schemaId : String) {
    val schema = Source.fromInputStream(req.getRequestBody()).mkString

    // If requests are handled in concurrently, this may result in a race
    // condition if two schemas of the same name are uploaded at the same
    // time.
    if ( templateStore.schemaExists(schemaId) ) {
      sendJsonResponse(req, 409, JObject(List(
        ("action",  JString("uploadSchema")),
        ("id",      JString(schemaId)),
        ("status",  JString("error")),
        ("message", JString("Schema already exists"))
      )))
    } else if ( !Validator.isValidJson(schema) ) {
      sendJsonResponse(req, 400, JObject(List(
        ("action",  JString("uploadSchema")),
        ("id",      JString(schemaId)),
        ("status",  JString("error")),
        ("message", JString("Invalid JSON"))
      )))
    } else {
      sendJsonResponse(req, 400, JObject(List(
        ("action",  JString("uploadSchema")),
        ("id",      JString(schemaId)),
        ("status",  JString("success"))
      )))
      templateStore.storeSchema(schemaId, schema)
    }
  }

  def handleValidateDocument(req : HttpExchange, schemaId : String) {
    val json = Source.fromInputStream(req.getRequestBody()).mkString

    ( templateStore.validateSchema(schemaId, json) ) match {
      case Some(failureString) =>
        sendJsonResponse(req, 200, JObject(List(
          ("action",  JString("validateDocument")),
          ("id",      JString(schemaId)),
          ("status",  JString("error")),
          ("message", JString(failureString))
        )))
      case None =>
        sendJsonResponse(req, 200, JObject(List(
          ("action", JString("validateDocument")),
          ("id",     JString(schemaId)),
          ("status", JString("success"))
        )))
    }
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
