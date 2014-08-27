import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._


organization := "io.suggest"

name := "sioweb21"

version := "1.0-SNAPSHOT"

lazy val web21 = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

scalaVersion := "2.10.4"
//scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc, 
  anorm,
  cache,      // play-2.2+
  json,       // play-2.3+
  ws,
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.mohiva" %% "play-html-compressor" % "0.4-SNAPSHOT",  // https://github.com/mohiva/play-html-compressor
  // io.suggest stuff
  "io.suggest" %% "util" % "1.4.0-SNAPSHOT" changing()
    exclude("org.jruby", "jruby-complete")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.slf4j", "log4j-over-slf4j")
    ,
  "io.suggest" %% "util-play" % "2.3.3-SNAPSHOT"
    exclude("org.jruby", "jruby-complete")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.slf4j", "log4j-over-slf4j")
  ,
  // для разбора upload-частей, нужна помощь mime magic bytes
  "jmimemagic" % "jmimemagic" % "0.1.2"
    exclude("xml-apis", "xml-apis")
    exclude("xml-apis", "xmlParserAPIs")
    exclude("xerces",   "xerces")
    exclude("log4j",    "log4j")
  ,
  "org.slf4j" % "log4j-over-slf4j" % "1.+",
  // coffeescript-компилятор используем свой заместо компилятора play по ряду причин (последний прибит гвоздями к sbt-plugin, например).
  "org.jcoffeescript" % "jcoffeescript" % "1.6-SNAPSHOT",
  // for domain validation:
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.+",
  "org.apache.httpcomponents" % "httpcore" % "4.1.+",
  "com.google.guava" % "guava" % "17.+",
  "com.lambdaworks" % "scrypt" % "1.4.0",     // Чтобы хешировать пароли (models.EmailPwIdent например)
  // Для импорт данных из sio v1 кластера. Выпилить вместе с util.compat.v1 после запуска
  "org.erlang.otp" % "jinterface" % "1.5.+",
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.4.+",
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  // Бомжуем с синхронным драйвером из-за конфликта между postgresql-async и asynchbase в версии netty. Зато anorm работает.
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  // webjars
  // geo
  "com.spatial4j" % "spatial4j" % "0.4.+",
  "com.vividsolutions" % "jts" % "1.13",
  // statistics
  "net.sf.uadetector" % "uadetector-resources" % "2014.+",
  // test
  "org.scalatestplus" %% "play" % "1.1.0" % "test"    // версию надо обновлять согласно таблице http://www.scalatest.org/plus/play/versions
)

play.Play.projectSettings


// Добавляем задачу сжатия всех сгенеренных js/css файлов.
lazy val gzipAssets = taskKey[Unit]("Gzip all js/css assets.")

gzipAssets := {
  val dir = resourceManaged.value
  println("Compressing js/ccs in directory: " + dir)
  ((dir ** "*.js") +++ (dir ** "*.css")).get.foreach { file =>
    val gzTarget = new File(file.getAbsolutePath + ".gz")
    IO.gzip(file, gzTarget)
    println("Compressed " + file.getName + " " + file.length / 1000F + " k => " + gzTarget.getName + " " + gzTarget.length / 1000F + " k")
    gzTarget
  }
}


// После импорта настроек, typesafe-репа не кешируется. Это надо бы исправить.
resolvers ~= {
  rs => rs filter {_.name != "Typesafe Releases Repository" }
}

// Добавить резолверы, в т.ч. кэш-резолвер для отфильтрованной выше репы.
resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com",
  "sonatype-groups-forge" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-groups-forge"
  //"sonatype-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)


//net.virtualvoid.sbt.graph.Plugin.graphSettings

routesImport ++= Seq(
  "models._",
  "util.qsb._",
  "util.qsb.QSBs._"
)


// Stylus
includeFilter in (Assets, StylusKeys.stylus) := "*.styl"

excludeFilter in (Assets, StylusKeys.stylus) := "_*.styl"

//StylusKeys.compress in Assets := true


// LESS
includeFilter in (Assets, LessKeys.less) := "bootstrap.less"


// sbt-web
//pipelineStages := Seq(rjs, gzip)
pipelineStages := Seq(gzip)

