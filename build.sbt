import play.sbt.PlayImport

Common.settingsOrg

name := "util"

version := "2.0.1-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots"
)


libraryDependencies ++= {
  val slf4jVsn      = "1.7.+"
  val esVsn         = "1.7.2"  // TODO "2.0.2"
  val akkaVsn       = "2.4.+"
  val morphVsn      = "1.3-SNAPSHOT"
  Seq(
    Common.ORG  %% "common"       % "0.0.0-SNAPSHOT",
    Common.ORG  %% "logs-macro"   % "0.0.0-SNAPSHOT",
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+",
    "org.gnu.inet" % "libidn" % "1.15",
    "com.github.nscala-time" %% "nscala-time" % "1.+",
    "org.apache.commons" % "commons-lang3" % "3.+",
    "org.im4java" % "im4java" % "1.+",
    // JSON
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.+",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.7.+",
    //"org.json4s" %% "json4s-native" % "3.+",
    PlayImport.json,
    PlayImport.ws,
    PlayImport.cache,
    // ES
    "org.elasticsearch" % "elasticsearch" % esVsn,
    // Полу-официальная поддержка GeoJSON для play:
    "com.typesafe.play.extras" %% "play-geojson" % "1.4.+",
    // play 2.5.1: AHC-2.0 там кривой RC16, https://github.com/AsyncHttpClient/async-http-client/issues/1123
    // TODO После 2.5.2 или 2.6.0 можно удалить, т.к. в git уже -RC19 проставлен.
    "org.asynchttpclient" % "async-http-client" % "2.0.0-RC19",
    // akka
    "com.typesafe.akka" %% "akka-actor"  % akkaVsn,
    "com.typesafe.akka" %% "akka-remote" % akkaVsn,
    // Морфология
    //"org.apache.lucene.morphology" % "russian" % morphVsn,
    //"org.apache.lucene.morphology" % "english" % morphVsn,
    // geo
    "com.spatial4j" % "spatial4j" % "0.4.+",
    "com.vividsolutions" % "jts" % "1.13",
    // TEST
    //"net.databinder.dispatch" %% "dispatch-core" % "0.11.+" % "test",
    "org.scalatest" %% "scalatest" % "2.2.+" % "test"   // Потом надо на 2.3 переключится.
  )
}

//testOptions in Test += Tests.Argument("-oF")

