name := "flipsys"
version := "1.0-SNAPSHOT"

scalaVersion := Versions.scala

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-Xlint"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.akka" %% "akka-actor" % Versions.akka,
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test,
  "org.scalameta" %% "munit" % "0.7.22" % Test,
  "com.fazecast" % "jSerialComm" % Versions.jSerialComm,
  "com.github.nscala-time" %% "nscala-time" % Versions.nScalaTime,
  "org.scalactic" %% "scalactic" % Versions.scalaTest,
  "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
)
