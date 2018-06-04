name := "headache"
version := "0.1-SNAPSHOT"

scalaVersion := "2.12.6"
fork := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Yno-adapted-args", "-Xlint", "-Ypartial-unification", "-opt:_", "-opt-warnings:_", "-Ywarn-extra-implicit", "-Ywarn-inaccessible", "-Ywarn-infer-any", "-Ywarn-nullary-override", "-Ywarn-nullary-unit", "-Ywarn-numeric-widen")
scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:_", "-opt:_", "-Xlint")

libraryDependencies ++= Seq(
  "org.asynchttpclient" % "async-http-client" % "2.0.33",
  "com.beachape" %% "enumeratum" % "1.5.12",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "org.json4s" %% "json4s-native" % "3.5.2",
  "org.scala-stm" %% "scala-stm" % "0.8",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.openjdk.jol" % "jol-core" % "0.9" % "test",
  "com.github.pathikrit" %% "better-files" % "3.4.0" % "test",
)
resolvers += "jitpack.io" at "https://jitpack.io"

