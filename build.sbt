name := "util"

organization := "io.suggest"

version := "1.2.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com"
)


libraryDependencies ++= {
  val slf4jVsn      = "1.7.+"
  val esVsn         = "1.1.1"
  val hadoopVsn     = "2.4.+"
  val hbaseVsn      = "0.98.1-hadoop2"
  val akkaVsn       = "2.3.+"
  val jacksonVsn    = "2.3.+"
  val tikaVsn       = "1.5"
  val cascadingVsn  = "2.5.+"
  val playVsn       = "2.3-SNAPSHOT"
  val morphVsn      = "1.2-SNAPSHOT"
  val bcVsn         = "1.46"
  Seq(
    "org.slf4j" % "slf4j-api" % slf4jVsn,
    "org.slf4j" % "slf4j-log4j12" % slf4jVsn,
    "com.typesafe" %% "scalalogging-slf4j" % "1.1.+",
    "org.gnu.inet" % "libidn" % "1.1+",
    "com.github.nscala-time" %% "nscala-time" % "0.+",
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
    "org.apache.hadoop" % "hadoop-main" % hadoopVsn,
    "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
    // hbase
    "org.apache.hbase" % "hbase" % hbaseVsn,
    "org.apache.hbase" % "hbase-server" % hbaseVsn,
    "org.apache.hbase" % "hbase-common" % hbaseVsn,
    "org.hbase" % "asynchbase" % "1.5.0A-SNAPSHOT",
    // cascading
    "cascading" % "cascading-core" % cascadingVsn,
    //"cascading" % "cascading-hadoop" % cascadingVsn,      // hadoop <= 1.x
    "cascading" % "cascading-hadoop2-mr1" % cascadingVsn,   // hadoop >= 2.x
    "com.scaleunlimited" % "cascading-utils" % "2.2sio-SNAPSHOT", // нужно для HadoopUtils.
    // Морфология
    "org.apache.lucene.morphology" % "russian" % morphVsn,
    "org.apache.lucene.morphology" % "english" % morphVsn,
    // TEST
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.+" % "test",
    "org.scalatest" %% "scalatest" % "2.+" % "test"
  )
}

// Нужно генерить текстовое дерево категорий на основе xls-списка категорий яндекс-маркета.
// Для этого надо скачать и распарсить xls-файл. Для работы нужна xls2csv.
resourceGenerators in Compile <+= Def.task {
  // Подготовка к созданию csv-файла
  val csvFileDir = (resourceManaged in Compile).value / "ym" / "cat"
  val csvFile = csvFileDir / "market_category.csv"
  if (!csvFile.exists) {
    val xlsFilename = "market_categories.xls"
    val xlsFile = target.value / xlsFilename
    if (!xlsFile.exists) {
      // xls-файл ещё не скачан. Нужно вызвать download сначала
      // Ссылка взята из http://help.yandex.ru/partnermarket/guides/clothes.xml
      val xlsUrl = "http://help.yandex.ru/partnermarket/docs/market_categories.xls" 
      println("Downloading " + xlsUrl + " ...")
      IO.download(new URL(xlsUrl), xlsFile)
      println("Downloaded " + xlsFile.toString + " OK")
    }
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
  Seq(csvFile)
}

