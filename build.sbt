import eu.diversit.sbt.plugin.WebDavPlugin._

name := "util"

organization := "io.suggest"

version := "0.5.4"


scalaVersion := "2.10.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")


seq(WebDav.globalSettings : _*)

publishMavenStyle := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("Server Ivy2 Repo" at  "https://ivy2-internal.cbca.ru/sbt/")


externalIvySettings(baseDirectory(_ / "project" / "ivysettings.xml"))

libraryDependencies ++= {
  val slf4jVsn  = "1.7.2"
  val hadoopVsn = "1.1.2"
  val akkaVsn   = "2.1.0"
  val jacksonVsn = "2.2.1"
  Seq(
    "org.scalatest" %% "scalatest" % "1.9.1" % "test",
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "org.gnu.inet" % "libidn" % "1.15",
    "com.github.nscala-time" %% "nscala-time" % "0.2.0",
    "commons-lang" % "commons-lang" % "2.4",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVsn,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVsn,
    "org.elasticsearch" % "elasticsearch" % "0.20.6",
    "org.apache.hadoop" % "hadoop-core" % hadoopVsn,
    "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
    "com.typesafe.akka" %% "akka-actor" % akkaVsn
  )}
