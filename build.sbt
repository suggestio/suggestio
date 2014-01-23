import eu.diversit.sbt.plugin.WebDavPlugin._

name := "util"

organization := "io.suggest"

version := "0.6.0-SNAPSHOT"


scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation")


seq(WebDav.globalSettings : _*)

publishMavenStyle := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("cbca ivy2-internal" at  "https://ivy2-internal.cbca.ru/sbt/")


externalIvySettings(baseDirectory(_ / "project" / "ivysettings.xml"))

libraryDependencies ++= {
  val slf4jVsn      = "1.7.5"
  val hadoopVsn     = "2.2.0"
  val hbaseVsn      = "0.96.1.1-hadoop2"
  val akkaVsn       = "2.3.0-RC1"
  val jacksonVsn    = "2.2.2"
  val tikaVsn       = "1.4"
  val cascadingVsn  = "2.5.1"
  Seq(
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "org.gnu.inet" % "libidn" % "1.15",
    "com.github.nscala-time" %% "nscala-time" % "0.2.0",
    "commons-lang" % "commons-lang" % "2.4",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVsn,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVsn,
    "org.elasticsearch" % "elasticsearch" % "0.90.10",
    "org.apache.tika" % "tika-core" % tikaVsn,
    "org.apache.tika" % "tika-parsers" % tikaVsn exclude("xerces", "xercesImpl"),
    // akka
    "com.typesafe.akka" %% "akka-actor"  % akkaVsn,
    "com.typesafe.akka" %% "akka-remote" % akkaVsn,
    "org.apache.hadoop" % "hadoop-main" % hadoopVsn,
    "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
    // hbase
    "org.apache.hbase" % "hbase" % hbaseVsn,
    "org.apache.hbase" % "hbase-server" % hbaseVsn,
    "org.apache.hbase" % "hbase-common" % hbaseVsn,
    "org.hbase" % "asynchbase" % "1.5.0A-SNAPSHOT",
    // cascading
    "cascading" % "cascading-core" % cascadingVsn,
    "cascading" % "cascading-hadoop" % cascadingVsn,
    "com.scaleunlimited" % "cascading.utils" % "2.2sio-SNAPSHOT", // нужно для HadoopUtils.
    // TEST
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.0" % "test",
    "org.scalatest" %% "scalatest" % "2.0" % "test"
  )
}

