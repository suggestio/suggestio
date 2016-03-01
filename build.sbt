import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._
import com.typesafe.sbt.SbtProguard.ProguardKeys._
import com.tuplejump.sbt.yeoman.Yeoman

Common.settingsOrg

name := "sioweb21"

version := "1.0-SNAPSHOT"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

//updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= {
 Seq(
  jdbc exclude("com.h2database", "h2"),
  "com.typesafe.play" %% "anorm" % "2.4.0",
  cache,
  json,
  "com.typesafe.play"   %% "play-slick" % Common.playSlickVsn,
  // slick повторно инклюдится здесь, т.к. что-то свежая версия не цеплялась через common-slick-driver
  "com.typesafe.slick"  %% "slick"      % Common.slickVsn,
  ws exclude("commons-logging", "commons-logging"),
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.mohiva" %% "play-html-compressor" % "0.5-SNAPSHOT",  // https://github.com/mohiva/play-html-compressor
  //"com.yahoo.platform.yui" % "yuicompressor" % "2.4.+",
  // io.suggest stuff
  Common.ORG %% "mbill2" % "0.0.0-SNAPSHOT",
  Common.ORG %% "svg-util" % "0.0.0-SNAPSHOT",
  Common.ORG %% "util" % "2.0.1-SNAPSHOT" changing()
    exclude("org.jruby", "jruby-complete")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.slf4j", "log4j-over-slf4j")
  ,
  Common.ORG %% "n2" % "0.0.0-SNAPSHOT",
  // для разбора upload-частей, нужна помощь mime magic bytes
  "jmimemagic" % "jmimemagic" % "0.1.2"
    exclude("xml-apis", "xml-apis")
    exclude("xml-apis", "xmlParserAPIs")
    exclude("xerces",   "xerces")
    exclude("log4j",    "log4j")
    exclude("commons-logging", "commons-logging")
  ,
  "com.ning" % "async-http-client" % "1.9.+",
  "org.slf4j" % "log4j-over-slf4j" % "1.+",
  // coffeescript-компилятор используем свой заместо компилятора play по ряду причин (последний прибит гвоздями к sbt-plugin, например).
  "org.jcoffeescript" % "jcoffeescript" % "1.6.2-SNAPSHOT",
  "com.google.guava" % "guava" % "18.+",
  "com.lambdaworks" % "scrypt" % "1.4.0",     // Чтобы хешировать пароли (models.EmailPwIdent например)
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.5.+",
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  // Бомжуем с синхронным драйвером из-за конфликта между postgresql-async и asynchbase в версии netty. Зато anorm работает.
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
  // webjars
  // geo
  "com.spatial4j" % "spatial4j" % "0.4.+",
  "com.vividsolutions" % "jts" % "1.13",
  //"com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.3.5",
  // statistics
  "net.sf.uadetector" % "uadetector-resources" % "2014.+",
  // scalasti - это простой гибкий динамический шаблонизатор строк. Нужен для генерации динамических карточек.
  "org.clapper" %% "scalasti" % "2.+",
  // bouncy castle используется для шифрования. pg используется для стойкого шифрования с подписью.
  "org.bouncycastle" % "bcpg-jdk15on"   % Common.bcVsn,
  "org.bouncycastle" % "bcmail-jdk15on" % Common.bcVsn,
  "org.bouncycastle" % "bcprov-jdk15on" % Common.bcVsn,
  "io.trbl.bcpg" % "bcpg-simple-jdk15on" % "1.51.0",
  // Логин через соц.сети
  Common.ORG %% "securesocial" % "3.4.0sio-SNAPSHOT"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  // спаривание guice и акторов требует танцев вприсядку
  //"net.codingwell" %% "scala-guice" % "4.0.0",
  "commons-io" % "commons-io" % "2.4",
  // test
  // play-2.3.x: Устарел selenium
  // org.w3c.css#sac конфликтует xml-apis-ext
  "org.fluentlenium" % "fluentlenium-festassert" % "0.10.2" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "org.fluentlenium" % "fluentlenium-core" % "0.10.2" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "org.seleniumhq.selenium" % "selenium-java" % "2.45.0" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "net.sourceforge.htmlunit" % "htmlunit-core-js" % "2.15" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
  ,
  // play-2.3+:
  "org.scalatestplus" %% "play" % "1.4.0-SNAPSHOT" % "test"
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
)}

// 2015.feb.26 Не надо этого делать, иначе в sio/2/Build.scala начинается тихий конфликт с доп.сеттингами оттуда.
//play.Play.projectSettings


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
  "models.im.ImOp._",
  "models.msc.AdCssArgs._",
  "io.suggest.mbill2.m.gid.Gid_t"
)


// Stylus
includeFilter in (Assets, StylusKeys.stylus) := "*.styl"

excludeFilter in (Assets, StylusKeys.stylus) := "_*.styl"



// sbt-web
// TODO rjs не нужен, но его очевидная замена на uglify калечит исходники css-ассетов в simpleUrlUpdate.
//pipelineStages ++= Seq(uglify, cssCompress, digest, simpleUrlUpdate, digest, filter, gzip)
pipelineStages ++= Seq(rjs, digest, simpleUrlUpdate, digest, filter, gzip)

//excludeFilter in simpleUrlUpdate := "*.md5"
//includeFilter in simpleUrlUpdate := "*.css"


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
routesGenerator := InjectedRoutesGenerator

Yeoman.yeomanSettings ++ Yeoman.withTemplates

// Вычистить гарантировано ненунжные ассеты. Они появляются из-за двойного вызова sbt-digest.
// У плагина sbt-filter почему-то фильтры наоборот работают. Об этом в README сказано.
// https://github.com/rgcottrell/sbt-filter
includeFilter in filter := {
  "*.md5.md5" || "*.scala.*" || "*.scala" ||
  "*.js.src.js" || "*.js.src.js.md5" ||
  "*.coffee" || "*.coffee.md5" || "*.coffee.map" ||
  "*.styl" || "*.styl.md5" ||
  "*.less" || "*.less.md5" ||
  "*-fastopt.js" || "*-fastopt.js.map" || "*-fastopt.js.md5" ||  // scala.js дебажный хлам в финальном билде не нужен.
  new PatternFilter("\\.*[a-f\\d]{32}-[a-f\\d]{32}-.+\\.[_\\w\\d.]+".r.pattern)   // Нужно фильтровать ассеты с двумя чексуммами в имени.
}


// Docker args. Ports: http, ssl, TODO set stable ports in elasticsearch.yml
dockerExposedPorts := Seq(9000, 9443, 9200, 9201, 9300, 9301)


// Есть ассеты, которые нет смысла сжимать. Правда, они в /public, но на всякий случай сделаем.
excludeFilter in gzip := "*.woff" || "*.woff2"

// Дополнительные импорты для twirl-шаблонов.
TwirlKeys.templateImports += "models.mctx.Context"

