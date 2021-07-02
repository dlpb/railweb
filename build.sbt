import com.typesafe.sbt.packager.MappingsHelper._

name := """railweb"""

version := "2.7.0"

scalaVersion := "2.13.3"

//crossScalaVersions := Seq("2.11.12", "2.12.7")

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies ++= Seq(
  "com.auth0" % "java-jwt" % "3.8.0",
  "org.json4s" %% "json4s-jackson" % "3.6.9",
  "org.json4s" %% "json4s-native" % "3.6.9",
  "org.postgresql" % "postgresql" % "42.2.1",
  "org.json4s" %% "json4s-ext" % "3.6.11"

)

libraryDependencies += ws
libraryDependencies += ehcache

libraryDependencies ++= Seq (
  "org.mockito" %% "mockito-scala" % "1.15.0" % "test",
  "org.mockito" % "mockito-core" % "3.5.13" % "test"
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)

javaOptions += "--add-opens java.base/jdk.internal.misc=ALL-UNNAMED"

javaOptions += "--illegal-access=warn"

mappings in Universal ++= directory(baseDirectory.value / "resources")