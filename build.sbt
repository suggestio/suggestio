import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._
import com.typesafe.sbt.SbtProguard.ProguardKeys._


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
  cache,
  json,
  ws,
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.mohiva" %% "play-html-compressor" % "0.4-SNAPSHOT",  // https://github.com/mohiva/play-html-compressor
  // io.suggest stuff
  "io.suggest" %% "util" % "1.6.0-SNAPSHOT" changing()
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
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.4.+",
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  // Бомжуем с синхронным драйвером из-за конфликта между postgresql-async и asynchbase в версии netty. Зато anorm работает.
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  // webjars
  "org.webjars" % "leaflet" % "0.7.3",
  "org.webjars" % "leaflet-markercluster" % "0.4.0",
  "org.webjars" % "leaflet-plugins" % "1.1.2",
  // geo
  "com.spatial4j" % "spatial4j" % "0.4.+",
  "com.vividsolutions" % "jts" % "1.13",
  // statistics
  "net.sf.uadetector" % "uadetector-resources" % "2014.+",
  // radius
  "org.tinyradius" % "tinyradius" % "1.0.3"
    exclude("commons-logging", "commons-logging")
  ,
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
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-releases",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com",
  "sonatype-groups-forge" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-groups-forge",
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots/",
  "websudos-releases" at "https://ivy2-internal.cbca.ru/artifactory/websudos-local-releases"
)



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
pipelineStages := Seq(rjs, gzip)
//pipelineStages := Seq(gzip)

testOptions in Test += Tests.Argument("-oF")


// Не генерить мусорную документацию во время stage/dist. Она не нужна никому.
sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false


// proguard: защита скомпиленного кода от реверса.
proguardSettings

ProguardKeys.options in Proguard ++= Seq(
  "-keepnames class * implements org.xml.sax.EntityResolver",
  """-keepclasseswithmembers public class * {
      public static void main(java.lang.String[]);
  }""",
  """-keepclassmembers class * {
      ** MODULE$;
  }""",
  """-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
      long eventCount;
      int  workerCounts;
      int  runControl;
      scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode syncStack;
      scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode spareStack;
  }""",
  """-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
      int base;
      int sp;
      int runState;
  }""",
  """-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
      int status;
  }""",
  """-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
      scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference head;
      scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference tail;
      scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference cleanMe;
  }""",
  "-dontnote",
  "-dontwarn",
  "-ignorewarnings"
)

ProguardKeys.options in Proguard += ProguardOptions.keepMain("play.core.server.NettyServer")

javaOptions in (Proguard, proguard) := Seq("-Xms512M", "-Xmx4G")

