import play.sbt.PlayImport._

Common.settingsOrg

name := "util"

version := "2.0.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  val slf4jVsn      = Common.Vsn.SLF4J 
  Seq(
    "org.gnu.inet" % "libidn" % "1.15",
    "org.apache.commons" % "commons-lang3" % Common.Vsn.COMMONS_LANG3,
    "org.im4java" % "im4java" % "1.+",

    // Parsers
    "org.apache.tika" % "tika-core" % Common.Vsn.TIKA,
    "org.apache.tika" % "tika-parsers" % Common.Vsn.TIKA
      exclude("xerces", "xercesImpl")
      exclude("org.bouncycastle", "bcmail-jdk15") // не нужно нам вскрывать зашифрованные архивы и pdf'ки.
      exclude("org.bouncycastle", "bcprov-jdk15")
      exclude("org.ow2.asm", "asm-debug-all")
      exclude("edu.ucar", "netcdf")
      exclude("commons-logging", "commons-logging")
      exclude("de.l3s.boilerpipe", "boilerpipe")
    ,
    "commons-io" % "commons-io" % Common.apacheCommonsIoVsn,

    // JSON
    "com.typesafe.play"            %% "play-json"            % Common.Vsn.PLAY_JSON_VSN,
    ws,
    cacheApi,
    // Какие-то проблемы с резолвом shaded-oauth через artifactory. Можно попробовать раскомментить typesafe-releases или это.
    //"com.typesafe.play" % "shaded-oauth" % "2.0.1",
   
    // CompressUtil использует это. И [www] тоже.
    "commons-io" % "commons-io" % Common.apacheCommonsIoVsn,
    // UuidUtil использует внешний Base64-кодек:
    "commons-codec" % "commons-codec" % Common.Vsn.APACHE_COMMONS_CODEC,

    // TEST
    "org.slf4j" % "slf4j-api" % slf4jVsn % Test,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn % Test,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

//testOptions in Test += Tests.Argument("-oF")

