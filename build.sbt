name := "headache"
version := "0.1-SNAPSHOT"

scalaVersion := "2.13.0"
fork := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-opt:_", "-opt-warnings:_", "-opt:l:inline", "-opt-inline-from:scala.**")
scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:_", "-opt:_", "-Xlint")

libraryDependencies ++= Seq(
  "org.asynchttpclient" % "async-http-client" % "2.10.0",
  "com.beachape" %% "enumeratum" % "1.5.13",
  "com.typesafe.play" %% "play-json" % "2.7.4",
  "org.json4s" %% "json4s-native" % "3.6.6",
  "org.scala-stm" %% "scala-stm" % "0.9.1",
  "org.slf4j" % "slf4j-simple" % "1.7.26" % "runtime",
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.openjdk.jol" % "jol-core" % "0.9" % "test",
  "com.github.pathikrit" %% "better-files" % "3.8.0" % "test",
)
resolvers += "jitpack.io" at "https://jitpack.io"

outputStrategy := Some(StdoutOutput)
