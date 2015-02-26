import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._
import com.typesafe.sbt.SbtProguard.ProguardKeys._
import com.tuplejump.sbt.yeoman.Yeoman


organization := "io.suggest"

name := "sioweb21"

version := "1.0-SNAPSHOT"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

scalaVersion := "2.11.5"

//updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  jdbc exclude("com.h2database", "h2"),
  anorm,
  cache,
  json,
  ws exclude("commons-logging", "commons-logging"),
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.4.0-M2-SNAPSHOT",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.mohiva" %% "play-html-compressor" % "0.4-SNAPSHOT",  // https://github.com/mohiva/play-html-compressor
  //"com.yahoo.platform.yui" % "yuicompressor" % "2.4.+",
  // io.suggest stuff
  "io.suggest" %% "util" % "2.0.0-SNAPSHOT" changing()
    exclude("org.jruby", "jruby-complete")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.slf4j", "log4j-over-slf4j")
  ,
  "io.suggest" %% "advext-common" % "0.0.0-SNAPSHOT",
  "io.suggest" %% "model-enum-util" % "0.0.0-SNAPSHOT",
  // для разбора upload-частей, нужна помощь mime magic bytes
  "jmimemagic" % "jmimemagic" % "0.1.2"
    exclude("xml-apis", "xml-apis")
    exclude("xml-apis", "xmlParserAPIs")
    exclude("xerces",   "xerces")
    exclude("log4j",    "log4j")
    exclude("commons-logging", "commons-logging")
  ,
  "org.slf4j" % "log4j-over-slf4j" % "1.+",
  // coffeescript-компилятор используем свой заместо компилятора play по ряду причин (последний прибит гвоздями к sbt-plugin, например).
  "org.jcoffeescript" % "jcoffeescript" % "1.6.2-SNAPSHOT",
  // for domain validation:
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.+",
  "org.apache.httpcomponents" % "httpcore" % "4.1.+",
  "com.google.guava" % "guava" % "18.+",
  "com.lambdaworks" % "scrypt" % "1.4.0",     // Чтобы хешировать пароли (models.EmailPwIdent например)
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.4.+",
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  // Бомжуем с синхронным драйвером из-за конфликта между postgresql-async и asynchbase в версии netty. Зато anorm работает.
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  // webjars
  // geo
  "com.spatial4j" % "spatial4j" % "0.4.+",
  "com.vividsolutions" % "jts" % "1.13",
  // statistics
  "net.sf.uadetector" % "uadetector-resources" % "2014.+",
  // radius
  "org.tinyradius" % "tinyradius" % "1.0.3"
    exclude("commons-logging", "commons-logging")
  ,
  // Явный дубляж из util, эти зависимости по факту нужны datastax-cassandra-драйверу, но он их не тянет.
  "org.xerial.snappy" % "snappy-java" % "1.+",
  "net.jpountz.lz4" % "lz4" % "1.+",
  // scalasti - это простой гибкий динамический шаблонизатор строк. Нужен для генерации динамических карточек.
  "org.clapper" %% "scalasti" % "2.+",
  // svg. batik довольно кривой, exclude(batik-ext) не пашет, приходится сочинять чудеса.
  "org.apache.xmlgraphics" % "batik-svg-dom" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-dom" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-css" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-xml" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-util" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-parser" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-anim" % "1.7" intransitive(),
  "org.apache.xmlgraphics" % "batik-awt-util" % "1.7" intransitive(),
  "xml-apis" % "xml-apis" % "1.4.01",
  "xml-apis" % "xml-apis-ext" % "1.3.04",
  // test
  // play-2.3.x: Устарел selenium
  // org.w3c.css#sac конфликтует xml-apis-ext
  "org.fluentlenium" % "fluentlenium-festassert" % "0.10.2"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "org.fluentlenium" % "fluentlenium-core" % "0.10.2"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "org.seleniumhq.selenium" % "selenium-java" % "2.43.1"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "net.sourceforge.htmlunit" % "htmlunit-core-js" % "2.15"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  // Логин через соц.сети
  "io.suggest" %% "securesocial" % "3.4.0-SNAPSHOT"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  // play-2.3+:
  "org.scalatestplus" %% "play" % "1.2.0play24-SNAPSHOT" % "test"    // версию надо обновлять согласно таблице http://www.scalatest.org/plus/play/versions
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
)

// 2015.feb.26 Не надо этого делать, иначе в sio/2/Build.scala начинается тихий конфликт с доп.сеттингами оттуда.
//play.Play.projectSettings


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
  "sonatype-oss-snapshots" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "websudos-releases" at "https://ivy2-internal.cbca.ru/artifactory/websudos-local-releases",
  "scalaz-bintray-repo" at "https://ivy2-internal.cbca.ru/artifactory/scalaz-bintray-repo"
)



routesImport ++= Seq(
  "models._",
  "util.qsb._",
  "util.qsb.QSBs._",
  "models.im.ImOp._"
)


// Stylus
includeFilter in (Assets, StylusKeys.stylus) := "*.styl"

excludeFilter in (Assets, StylusKeys.stylus) := "_*.styl"



// sbt-web
pipelineStages := Seq(rjs, digest, simpleUrlUpdate, digest, filter, gzip)

testOptions in Test += Tests.Argument("-oF")


// Не генерить мусорную документацию во время stage/dist. Она не нужна никому.
sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

// org.apache.xmlbeans требует сие зависимостью. иначе proguard не пашет.
unmanagedJars in Compile ~= {uj => 
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

// proguard: защита скомпиленного кода от реверса.
proguardSettings

ProguardKeys.proguardVersion in Proguard := "5.2"

ProguardKeys.options in Proguard ++= Seq(
  // https://gist.github.com/cessationoftime/4029263
  "-keep class * implements java.sql.Driver",
  "-keep class scala.concurrent.stm.ccstm.CCSTM { *; }",
  "-keep class akka.remote.RemoteActorRefProvider { *; }",
  "-keep class akka.event.Logging$DefaultLogger { *; }",
  "-keep class akka.event.Logging$LogExt { *; }",
  "-keep class akka.event.slf4j.Slf4jEventHandler { *; }",
  "-keep class akka.remote.netty.NettyRemoteTransport { *; }",
  "-keep class akka.serialization.JavaSerializer { *; }",
  "-keep class akka.serialization.ProtobufSerializer { *; }",
  "-keep class com.google.protobuf.* { *; }" ,
  // etc
  "-keep class * extends javax.xml.parsers.SAXParserFactory",
  "-keep public class org.apache.xerces.**",
  "-keep public class play.api.**",
  "-keep public class ch.qos.logback.**",
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
  "-keep class * implements models.ai.ContentHandlerResult",
  "-keepattributes *Annotation*,Signature",
  // http://stackoverflow.com/a/10311980
  "-keep class com.google.inject.Binder",
  "-keep public class com.google.inject.Inject",
  // keeps all fields and Constructors with @Inject
  """-keepclassmembers,allowoptimization,allowobfuscation class * {
    @com.google.inject.Inject <fields>;
    @com.google.inject.Inject <init>(...);
  }""",
  """-keepclassmembers,allowobfuscation,allowoptimization class * {
    @com.google.inject.Provides <methods>;
  }""",
  // TODO заценить описалово из http://stackoverflow.com/a/5843875
  "-optimizations !method/inlining/*",
  "-dontoptimize",
  "-dontobfuscate",
  //"-ignorewarnings",
  "-verbose",
  "-dontnote",
  "-dontwarn"
)

ProguardKeys.options in Proguard += ProguardOptions.keepMain("play.core.server.NettyServer")

javaOptions in (Proguard, proguard) := Seq("-Xms512M", "-Xmx4G")

// play-2.4: нужно устранить всякие import controllers... из шаблонов и иных мест.
//routesGenerator := InjectedRoutesGenerator

Yeoman.yeomanSettings ++ Yeoman.withTemplates

// Вычистить гарантировано ненунжные ассеты. Они появляются из-за двойного вызова sbt-digest.
// У плагина sbt-filter почему-то фильтры наоборот работают. Об этом в README сказано.
// https://github.com/rgcottrell/sbt-filter
includeFilter in filter := {
  "*.md5.md5" || "*.scala.*" ||
  "*.js.src.js" || "*.js.src.js.md5" ||
  "*.coffee" || "*.coffee.md5" || "*.coffee.map" ||
  "*.styl" || "*.styl.md5" ||
  "*.less" || "*.less.md5" ||
  "*-fastopt.js" || "*-fastopt.js.map" || "*-fastopt.js.md5" ||  // scala.js дебажный хлам в финальном билде не нужен.
  new PatternFilter("\\.*[a-f\\d]{32}-[a-f\\d]{32}-.+\\.[_\\w\\d.]+".r.pattern)   // Нужно фильтровать ассеты с двумя чексуммами в имени.
}

