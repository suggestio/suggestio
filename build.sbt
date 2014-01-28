name := "util"

organization := "io.suggest"

version := "0.6.0-SNAPSHOT"


scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation")


publishMavenStyle := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := Some("cbca-corp-repo" at "https://ivy2-internal.cbca.ru/artifactory/corp-repo/")

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
    "com.github.tototoshi" %% "scala-csv" % "1.0.0-SNAPSHOT",
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

// Нужно генерить текстовое дерево категорий на основе xls-списка категорий маркета.
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
    // TODO Надо что-то с ковычками вокруг аргументов тут решить. Если любой путь содержит пробел, то будет ошибка.
    val cmd = s"xls2csv -q -x ${xlsFile.toString} -b WINDOWS-1251 -c ${csvFile.toString} -a UTF-8"
    println("Creating CSV categories tree from. Executing shell command:\n " + cmd)
    cmd.!!
  }
  Seq(csvFile)
}

