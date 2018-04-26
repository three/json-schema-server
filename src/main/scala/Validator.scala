package jsonvalidator

import collection.JavaConverters._

import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import com.fasterxml.jackson.databind.node.{JsonNodeType, JsonNodeFactory}
import com.fasterxml.jackson.core.JsonParseException
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory

object Validator {
  /**
   * Parses schema and json, cleans json and tests against schema. Give None
   * if success, Some(message) if anything fails
   */
  def validateJson(schema : String, json : String) : Option[String] = {
    try {
      val mapper = new ObjectMapper()
      val parsedSchema : JsonNode = mapper.readTree(schema)
      val parsedJson : JsonNode = mapper.readTree(json)
      val cleanedJson = cleanJson(parsedJson)

      val factory = JsonSchemaFactory.byDefault()
      val validator = factory.getJsonSchema(parsedSchema)
      val report = validator.validate(cleanedJson)

      var validated = true
      var message = ""
      report.forEach( e => {
        validated = false
        message += e.toString() + "\n"
      })

      if ( validated ) {
        None
      } else {
        Some(message)
      }
    } catch {
      case e : JsonParseException => Some("Unable to parse JSON: " + e.toString())
    }
  }

  def isValidJson(json : String) : Boolean = {
    val mapper = new ObjectMapper()
    try {
      mapper.readTree(json).getNodeType != JsonNodeType.MISSING
    } catch {
      case e : JsonParseException => false
    }
  }

  /**
   * Removes all NULL fields from properties and all NULL values from arrays
   */
  def cleanJson(json : JsonNode) : JsonNode = {
    if ( json.getNodeType == JsonNodeType.OBJECT ) {
      var node =  JsonNodeFactory.instance.objectNode()
      json.fields.asScala.foreach(m => {
        if ( m.getValue.getNodeType != JsonNodeType.NULL ) {
          node.set(m.getKey, cleanJson(m.getValue))
        }
      })
      node
    } else if ( json.getNodeType == JsonNodeType.ARRAY ) {
      var node = JsonNodeFactory.instance.arrayNode()
      json.elements.asScala.foreach(n => {
        if ( n.getNodeType != JsonNodeType.NULL ) {
          node.add(cleanJson(n))
        }
      })
      node
    } else {
      json
    }
  }
}
