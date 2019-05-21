import com.typesafe.sbt.packager.MappingsHelper._

name := """play-scala-forms-example"""

version := "2.6.0"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.12", "2.12.7")

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies ++= Seq(
  "com.auth0" % "java-jwt" % "3.8.0",
  "org.json4s" %% "json4s-jackson" % "3.6.5",
  "org.json4s" %% "json4s-native" % "3.6.5",
  "org.postgresql" % "postgresql" % "42.2.1"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)

mappings in Universal ++= directory(baseDirectory.value / "resources")