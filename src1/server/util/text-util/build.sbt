import play.sbt.PlayImport

Common.settingsOrg

name := "text-util"

version := "0.0.0"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "org.apache.commons" % "commons-lang3" % Common.Vsn.COMMONS_LANG3,
    "commons-codec" % "commons-codec" % "1.+",

    // Импортируем Lucene с помощью импорта ElasticSearch. Глупо, но так удобнее.
    "org.elasticsearch" % "elasticsearch" % Common.Vsn.ELASTIC_SEARCH,

    // Морфология
    //"org.apache.lucene.morphology" % "russian" % morphVsn,
    //"org.apache.lucene.morphology" % "english" % morphVsn,
    "org.scala-lang.modules" %% "scala-parser-combinators" % Common.Vsn.SCALA_PARSER_COMBINATORS,

    // TEST
    //"org.slf4j" % "slf4j-api" % slf4jVsn % Test,
    //"org.slf4j" % "slf4j-log4j12" % slf4jVsn % Test,
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % Test
  )
}

//testOptions in Test += Tests.Argument("-oF")

