lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.4"
    ))
  )

resolvers += "emueller-bintray" at "http://dl.bintray.com/emueller/maven"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.0-M3"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.4"
libraryDependencies += "com.github.java-json-tools" % "json-schema-validator" % "2.2.8"
