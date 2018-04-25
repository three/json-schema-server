package jsonvalidator

import scala.{Option, Some}
import scala.io.Source
import java.io.FileNotFoundException

class TemplateStore(baseDirectory : String) {
  def isValidSchemaId(schemaId : String) = {
    val regex = """^[A-Za-z0-9_-]{1,}$""".r
    !regex.findFirstIn(schemaId).isEmpty
  }

  def templatePathFromId(schemaId : String) : Option[String] = {
    if ( isValidSchemaId(schemaId) ) {
      new Some( concatPath(baseDirectory, schemaId + ".json") )
    } else {
      None
    }
  }

  def getTemplateSource(schemaId : String) : Option[Source] = {
    templatePathFromId(schemaId).flatMap(sourceFromFileIfExists)
  }

  private def concatPath(p1 : String, p2 : String) = p1 + "/" + p2

  private def sourceFromFileIfExists(fname: String) : Option[Source] = {
    try {
      Some(Source.fromFile(fname))
    } catch {
      case e: FileNotFoundException => None
    }
  }
}
