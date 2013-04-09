name := "util"

version := "0.1"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers += "Conjars" at "http://conjars.org/repo"

organization := "io.suggest"


libraryDependencies ++= {
  val slf4jVsn  = "1.7.2"
  Seq(
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "org.gnu.inet" % "libidn" % "1.15",
    "com.github.nscala-time" %% "nscala-time" % "0.2.0",
    "commons-lang" % "commons-lang" % "2.4",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.1.3",
    "org.elasticsearch" % "elasticsearch" % "0.20.6"
  )}
