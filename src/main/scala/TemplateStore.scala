package jsonvalidator

import java.io.File
import java.nio.file.{Paths, Files}
import scala.{Option, Some}
import scala.io.Source
import java.io.FileNotFoundException
import org.json4s.JNothing
import org.json4s.ParserUtil
import org.json4s.jackson.JsonMethods

/**
 * Manages schemas stored in baseDirectory
 */
class TemplateStore(baseDirectory : String) {
  def isValidSchemaId(schemaId : String) = {
    val regex = """^[A-Za-z0-9_-]{1,}$""".r
    !regex.findFirstIn(schemaId).isEmpty
  }

  /**
   * Given a valid schemaId, returns Some(path) where path is where the schema
   * would be stored if it existed. An invalid schema will return None.
   */
  def templatePathFromId(schemaId : String) : Option[String] = {
    if ( isValidSchemaId(schemaId) ) {
      // Because all valid schemas (matching the regex in isValidSchema) are
      // valid filenames, no further processing is necessary.
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

  def getSchemaSource(schemaId : String) : Option[Source] = {
    templatePathFromId(schemaId).flatMap(sourceFromFileIfExists)
  }

  /**
   * Attempts to store schema, throws an error if invalid or exists
   */
  def storeSchema(schemaId : String, schema : String) {
    val schemaPath = try {
      Paths.get( templatePathFromId(schemaId).get )
    } catch {
      case e : NoSuchElementException =>
        throw new RuntimeException("Attempt to store schema with invalid name")
    }

    Files.write(schemaPath, schema.getBytes)
  }

  /**
   * Given valid schemaid, returns None if json validates against corresponding
   * schema, and Some(message) if doesn't parse, doesn't validate or schema
   * doesn't exist.
   */
  def validateSchema(schemaId : String, json : String) : Option[String] = {
    getSchemaSource(schemaId) match {
      case Some(schemaSource) =>
        Validator.validateJson(schemaSource.mkString, json)
      case None =>
        Some("Schema Not Found")
    }
  }

  // Assumes paths are valid and correctly formatted. A better solution would
  // be to use Java Paths instead
  private def concatPath(p1 : String, p2 : String) = p1 + "/" + p2

  private def sourceFromFileIfExists(fname: String) : Option[Source] = {
    try {
      Some(Source.fromFile(fname))
    } catch {
      case e: FileNotFoundException => None
    }
  }
}
