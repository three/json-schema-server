package jsonvalidator

import scala.Option
import scala.io.Source
import java.io.FileNotFoundException
import java.io.{InputStream, OutputStream}
import org.json4s.{JString, JInt}
import org.json4s.jackson.JsonMethods

object JsonValidator {
  def main(args: Array[String]) {
    val configSource : Source = {
      try {
        Source.fromFile("config.json")
      } catch {
        case e: FileNotFoundException =>
          throw new RuntimeException(
            "Unable to find config file. Check config.json exists in the current directory.")
      }
    }
    
    val config : Config = readConfig(configSource) match {
      case Some(config) => config
      case None =>
        throw new RuntimeException(
          "Invalid Config. Check config.json is well-formatted with storagePath and port fields")
    }

    println("Json Validator")
    println(" Schema Storage Directory: "+config.storagePath)
    println(" Port: "+config.port)

    val templateStore = new TemplateStore(config.storagePath)
    Server.start(config.port, templateStore)
  }

  /**
   * Read JSON-formatted configuration from Source
   */
  def readConfig(source : Source) : Option[Config] = {
    val json = JsonMethods.parse(source.mkString)

    (json \ "storagePath", json \ "port") match {
      case (JString(path), JInt(port)) => Some(Config(path, port.toInt))
      case default => None
    }
  }
}

case class Config(storagePath : String, port : Int)
