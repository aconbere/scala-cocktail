import AssemblyKeys._

assemblySettings

name := "cocktail"

version := "0.0.1"

scalaVersion := "2.10.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

fork in run := true

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.conbere" %% "irc" % "0.2.0",
  "org.conbere" %% "markov" % "0.2.0",
  "junit" % "junit" % "4.10" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.1.0",
  "org.rogach" %% "scallop" % "0.9.1",
  "com.github.spullara.mustache.java" % "compiler" % "0.8.12"
)

testOptions in Test += Tests.Argument("-oDF")
