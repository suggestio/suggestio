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
    "apache-releases"         at APACHE_RELEASES_URL
  )
}


libraryDependencies ++= {
  val slf4jVsn      = Common.Vsn.SLF4J 
  //val akkaVsn       = "2.5.+"
  //val morphVsn      = "1.3-SNAPSHOT"
  Seq(
    "org.gnu.inet" % "libidn" % "1.15",
    "org.apache.commons" % "commons-lang3" % Common.Vsn.COMMONS_LANG3,
    "org.im4java" % "im4java" % "1.+",

    // JSON
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.+",
    "com.typesafe.play"            %% "play-json"            % Common.Vsn.PLAY_JSON_VSN,
    PlayImport.ws,
    PlayImport.cacheApi,
   
    // CompressUtil использует это. И [www] тоже.
    "commons-io" % "commons-io" % Common.apacheCommonsIoVsn,

    // TEST
    "org.slf4j" % "slf4j-api" % slf4jVsn % Test,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn % Test,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

//testOptions in Test += Tests.Argument("-oF")

