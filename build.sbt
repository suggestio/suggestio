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
  val tikaVsn       = "1.7"
  val cascadingVsn  = "2.6.3"
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
    "org.json4s" %% "json4s-native" % "3.+",
    PlayImport.json,
    PlayImport.ws,
    PlayImport.cache,
    // ES
    "org.elasticsearch" % "elasticsearch" % esVsn,
    // Parsers
    "org.apache.tika" % "tika-core" % tikaVsn,
    "org.apache.tika" % "tika-parsers" % tikaVsn
      exclude("xerces", "xercesImpl")
      exclude("org.bouncycastle", "bcmail-jdk15") // не нужно нам вскрывать зашифрованные архивы и pdf'ки.
      exclude("org.bouncycastle", "bcprov-jdk15")
      exclude("org.ow2.asm", "asm-debug-all")
      exclude("edu.ucar", "netcdf")
      exclude("commons-logging", "commons-logging")
      exclude("de.l3s.boilerpipe", "boilerpipe")
    ,
    // Полу-официальная поддержка GeoJSON для play:
    "com.typesafe.play.extras" %% "play-geojson" % "1.4.+",
    // Для разбора csv от яндекс-маркета используем сий простой парсер, т.к. tika смотрит на всё, как на веб-страницу.
    "com.github.tototoshi" %% "scala-csv" % "1.0.0",
    // play 2.5.1: AHC-2.0 там кривой RC16, https://github.com/AsyncHttpClient/async-http-client/issues/1123
    // TODO После 2.5.2 или 2.6.0 можно удалить, т.к. в git уже -RC19 проставлен.
    "org.asynchttpclient" % "async-http-client" % "2.0.0-RC19",
    // akka
    "com.typesafe.akka" %% "akka-actor"  % akkaVsn,
    "com.typesafe.akka" %% "akka-remote" % akkaVsn,
    // cascading
    "cascading" % "cascading-core" % cascadingVsn,
    // Морфология
    "org.apache.lucene.morphology" % "russian" % morphVsn,
    "org.apache.lucene.morphology" % "english" % morphVsn,
    // geo
    "com.spatial4j" % "spatial4j" % "0.4.+",
    "com.vividsolutions" % "jts" % "1.13",
    // TEST
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.+" % "test",
    "org.scalatest" %% "scalatest" % "2.2.+" % "test"   // Потом надо на 2.3 переключится.
  )
}

// Скачивание файла с категориями яндекс.маркета и его парсинг доступен по команде из консоли updateMarketCategoriesCsv.
// Это перезапишет файл категорий в src/main/resources.
lazy val updateMarketCategoriesCsv = taskKey[Int]("Download and parse market_categories.xml into resource_managed.")

updateMarketCategoriesCsv := {
  val mcCsvFileName = "market_category.csv"
  val xlsFilename = "market_categories.xls"
    val xlsFile = target.value / xlsFilename
    if (!xlsFile.exists) {
      // xls-файл ещё не скачан. Нужно вызвать download сначала
      // Ссылка взята из http://help.yandex.ru/partnermarket/guides/clothes.xml
      //val xlsUrl = "http://help.yandex.ru/partnermarket/docs/market_categories.xls"
      val xlsUrl = "http://help.yandex.ru/help.yandex.ru/partnermarket/docs/market_categories.xls"
      println("Downloading " + xlsUrl + " ...")
      IO.download(new URL(xlsUrl), xlsFile)
      println("Downloaded " + xlsFile.toString + " OK")
    }
    val csvFileDir = (resourceDirectory in Compile).value / "ym" / "cat"
    val csvFile = csvFileDir / mcCsvFileName
    // Сгенерить на основе вендового xls-файла хороший годный csv-ресурс
    if (!csvFileDir.isDirectory) {
      csvFileDir.mkdirs()
      println(csvFileDir.toString + " dirs created")
    }
    // TODO Надо разобраться с зоопарком xls2csv.
    val cmd0 = List("/usr/bin/vendor_perl/xls2csv", "-q", "-x", xlsFile.toString, "-b", "WINDOWS-1251", "-c", csvFile.toString, "-a", "UTF-8")
    println("Creating CSV categories tree from. Executing shell command:\n " + cmd0.mkString(" "))
    cmd0 !
    // 2014.02.28: Яндекс поменял формат своей доки. Теперь всё добро свалено в одном столбце, и появилась корневая категория "Все товары".
    val cmd1 = List("sed", "-e", "s@ / @/@g", "-e", "s/^\"//", "-e", "s/\"$//", "-i", csvFile.toString)
    cmd1 !
}

//testOptions in Test += Tests.Argument("-oF")

