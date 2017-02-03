import play.sbt.PlayImport

Common.settingsOrg

name := "util"

version := "2.0.1-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= {
  import Common.Repo._
  Seq(
    "typesafe-releases"       at TYPESAFE_RELEASES_URL,
    "sonatype-oss-releases"   at SONATYPE_OSS_RELEASES_URL,
    "apache-releases"         at APACHE_RELEASES_URL,
    //"conjars-repo"          at CONJARS_REPO_URL,
    //"maven-twttr-com"       at MAVEN_TWTTR_COM_URL,
    "sonatype-oss-snapshots"  at SONATYPE_OSS_SNAPSHOTS_URL
  )
}


libraryDependencies ++= {
  val slf4jVsn      = Common.Vsn.SLF4J 
  val akkaVsn       = "2.4.+"
  val morphVsn      = "1.3-SNAPSHOT"
  Seq(
    //Common.ORG  %% "commonJVM"    % "0.0.0-SNAPSHOT",
    Common.ORG  %% "logs-macro"   % "0.0.0-SNAPSHOT",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+",
    "org.gnu.inet" % "libidn" % "1.15",
    "org.apache.commons" % "commons-lang3" % "3.+",
    "org.im4java" % "im4java" % "1.+",

    // Date-time
    "org.threeten" % "threeten-extra" % "1.0",

    // JSON
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.+",
    PlayImport.json,
    PlayImport.ws,
    PlayImport.cache,

    // ElasticSearch (Lucene):
    "org.elasticsearch" % "elasticsearch" % Common.Vsn.ELASTIC_SEARCH,
    //"com.sksamuel.elastic4s" %% "elastic4s-core"    % "2.4.0",
    //"com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",

    // akka
    "com.typesafe.akka" %% "akka-actor"  % akkaVsn,
    "com.typesafe.akka" %% "akka-remote" % akkaVsn,

    // Морфология
    //"org.apache.lucene.morphology" % "russian" % morphVsn,
    //"org.apache.lucene.morphology" % "english" % morphVsn,

    // TEST
    "org.slf4j" % "slf4j-api" % slf4jVsn % Test,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn % Test,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

//testOptions in Test += Tests.Argument("-oF")

