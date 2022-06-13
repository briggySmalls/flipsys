name := "flipsys"
version := "1.0-SNAPSHOT"

scalaVersion := Versions.scala
semanticdbEnabled := true // enable SemanticDB

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
  "com.typesafe.akka" %% "akka-remote" % Versions.akka,
  "com.typesafe.akka" %% "akka-cluster-typed" % Versions.akka,
  "com.typesafe.akka" %% "akka-serialization-jackson" % Versions.akka,
  "org.scalameta" %% "munit" % "0.7.22" % Test,
  "com.fazecast" % "jSerialComm" % Versions.jSerialComm,
  "com.github.nscala-time" %% "nscala-time" % Versions.nScalaTime,
  "com.googlecode.lanterna" % "lanterna" % Versions.lanterna,
  "org.scalactic" %% "scalactic" % Versions.scalaTest,
  "org.scalatest" %% "scalatest" % Versions.scalaTest % "test",
  "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % "test",
  "com.pi4j" % "pi4j-core" % Versions.py4j
)
