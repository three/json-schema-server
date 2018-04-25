package jsonvalidator

import java.io.File
import java.nio.file.{Paths, Files}
import scala.{Option, Some}
import scala.io.Source
import java.io.FileNotFoundException
import org.json4s.JNothing
import org.json4s.ParserUtil
import org.json4s.jackson.JsonMethods

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

  def schemaExists(schemaId : String) : Boolean = {
    templatePathFromId(schemaId) match {
      case Some(path) => (new File(path)).isFile
      case None       => false
    }
  }

  def isSchemaValid(schema : String) : Boolean = {
    try {
      JsonMethods.parse(schema) != JNothing
    } catch {
      case e : ParserUtil.ParseException => false
    }
  }

  def getTemplateSource(schemaId : String) : Option[Source] = {
    templatePathFromId(schemaId).flatMap(sourceFromFileIfExists)
  }

  def storeSchema(schemaId : String, schema : String) {
    val schemaPath = try {
      Paths.get( templatePathFromId(schemaId).get )
    } catch {
      case e : NoSuchElementException =>
        throw new RuntimeException("Attempt to store schema with invalid name")
    }

    Files.write(schemaPath, schema.getBytes)
  }

  def validateSchema(schemaId : String, json : String) : Option[String] = {
    val schema = getTemplateSource(schemaId).get.mkString
    Validator.validateJson(schema, json)
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
