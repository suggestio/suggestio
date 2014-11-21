name := "util"

organization := "io.suggest"

version := "1.10.5-SNAPSHOT"

scalaVersion := "2.10.4"
//scalaVersion := "2.11.3"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "websudos-releases" at "https://ivy2-internal.cbca.ru/artifactory/websudos-local-releases"
)


libraryDependencies ++= {
  val slf4jVsn      = "1.7.7"
  val esVsn         = "1.3.2"
  val hadoopVsn     = "2.4.1"
  val hbaseVsn      = "0.98.5-hadoop2"
  val akkaVsn       = "2.3.4"
  val jacksonVsn    = "2.4.+"
  val tikaVsn       = "1.6"
  val cascadingVsn  = "2.6.+"
  val playVsn       = "2.3.4"
  val morphVsn      = "1.3-SNAPSHOT"
  val bcVsn         = "1.46"
  val phantomVersion = "1.2.8"
  Seq(
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.+",
    "org.gnu.inet" % "libidn" % "1.15",
    "com.github.nscala-time" %% "nscala-time" % "1.+",
    "commons-lang" % "commons-lang" % "2.+",
    "org.im4java" % "im4java" % "1.+",
    // JSON
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVsn,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVsn,
    "org.json4s" %% "json4s-native" % "3.+",
    "com.typesafe.play" %% "play-json" % playVsn,
    // ES
    "org.elasticsearch" % "elasticsearch" % esVsn,
    // Parsers
    "org.apache.tika" % "tika-core" % tikaVsn,
    "org.apache.tika" % "tika-parsers" % tikaVsn
      exclude("xerces", "xercesImpl")
      exclude("org.bouncycastle", "bcmail-jdk15") // заменим ниже на *-jdk16
      exclude("org.bouncycastle", "bcprov-jdk15")
    ,
    // apache tika хочет bouncycastle для вскрытия негодных pdf'ов.
    "org.bouncycastle" % "bcmail-jdk16" % bcVsn,
    "org.bouncycastle" % "bcprov-jdk16" % bcVsn,
    // Для разбора csv от яндекс-маркета используем сий простой парсер, т.к. tika смотрит на всё, как на веб-страницу.
    "com.github.tototoshi" %% "scala-csv" % "1.0.0",
    // akka
    "com.typesafe.akka" %% "akka-actor"  % akkaVsn,
    "com.typesafe.akka" %% "akka-remote" % akkaVsn,
    "org.apache.hadoop" % "hadoop-main" % hadoopVsn
      exclude("commons-beanutils", "commons-beanutils")
    ,
    "org.apache.hadoop" % "hadoop-client" % hadoopVsn
      exclude("commons-beanutils", "commons-beanutils")
    ,
    // cassandra
    "com.websudos"  %% "phantom-dsl" % phantomVersion,
    "org.xerial.snappy" % "snappy-java" % "1.+",
    "net.jpountz.lz4" % "lz4" % "1.+",
    // hbase
    "org.apache.hbase" % "hbase" % hbaseVsn,
    "org.apache.hbase" % "hbase-server" % hbaseVsn
      exclude("org.mortbay.jetty", "jetty")
      exclude("org.mortbay.jetty", "jetty-util")
      exclude("org.mortbay.jetty", "jetty-sslengine")
      exclude("org.mortbay.jetty", "jsp-2.1")
      exclude("org.mortbay.jetty", "jsp-api-2.1")
      exclude("org.mortbay.jetty", "servlet-api-2.5")
      exclude("tomcat", "jasper-compiler")
      exclude("tomcat", "jasper-runtime")
    ,
    "org.apache.hbase" % "hbase-common" % hbaseVsn,
    "org.hbase" % "asynchbase" % "1.5.0A-SNAPSHOT",
    // cascading
    "cascading" % "cascading-core" % cascadingVsn,
    //"cascading" % "cascading-hadoop" % cascadingVsn,      // hadoop <= 1.x
    "cascading" % "cascading-hadoop2-mr1" % cascadingVsn,   // hadoop >= 2.x
    "com.scaleunlimited" % "cascading-utils" % "2.2sio-SNAPSHOT"   // нужно для HadoopUtils.
      exclude("cascading", "cascading-hadoop")
    ,
    // Морфология
    "org.apache.lucene.morphology" % "russian" % morphVsn,
    "org.apache.lucene.morphology" % "english" % morphVsn,
    // geo
    "com.spatial4j" % "spatial4j" % "0.4.+",
    "com.vividsolutions" % "jts" % "1.13",
    // TEST
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.+" % "test",
    "org.scalatest" %% "scalatest" % "2.+" % "test"
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

