//import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.Import.WebKeys.deduplicators

Common.settingsOrg

// npmAssets в Build.scala тихо конфликтует с этой строкой. ЕЁ НЕЛЬЗЯ РАСКОММЕНЧИВАТЬ, ТУТ КАК НАПОМИНАНИЕ.
//enablePlugins(WebScalaJSBundlerPlugin)

name := "www"

version := "1.0"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

//updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  //jdbc exclude("com.h2database", "h2"),
  guice,
  caffeine,
  "com.typesafe.play" %% "play-json" % Common.Vsn.PLAY_JSON_VSN,

  ws exclude("commons-logging", "commons-logging"),
  
  // Поддержка отслыки почты.
  "com.typesafe.play" %% "play-mailer" % Common.playMailerVsn,
  "com.typesafe.play" %% "play-mailer-guice" % Common.playMailerVsn,

  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.github.fkoehler" %% "play-html-compressor" % "1.0.0",
  // io.suggest stuff
  Common.ORG %% "util" % "2.0.1"
    exclude("org.jruby", "jruby-complete")
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("log4j", "log4j")
    exclude("org.slf4j", "log4j-over-slf4j")
  ,
  
  // TODO DateTimePrettyPrinter -- последний компонент в проекте, который тянет joda-time:
  "joda-time"             %  "joda-time"            % "2.8.+",

  //"com.ning" % "async-http-client" % "1.9.+",   // 2.5 migration, ahc -> 2.0.x. Удалить, если не понадобится возвращать.
  "org.slf4j" % "log4j-over-slf4j" % Common.Vsn.SLF4J,
  "com.google.guava" % "guava" % "18.+",
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.5.+",
  
  // Капчевание:
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  //"com.nappin" %% "play-recaptcha" % "2.4sio",

  "org.postgresql" % "postgresql" % "42.+",
  // statistics
  "net.sf.uadetector" % "uadetector-resources" % "2014.+",
  // scalasti - это простой гибкий динамический шаблонизатор строк. Нужен для генерации динамических карточек.
  //"org.clapper" %% "scalasti" % "2.+",

  // Для будущей работы с файлами добавлено вот это:
  //"com.github.pathikrit" %% "better-files"       % Common.Vsn.BETTER_FILES,

  // Валидация: по идее это должно быть на уровне common, но scala-2.12 пока не пашет, оно тут:
  //"com.wix"      %% "accord-core"     % Common.wixAccordVsn,

  // Поддержка JsMessages для client-side локализации сообщений
  "org.julienrf" %% "play-jsmessages" % "5.0.0",

  // jsRevRouter используется специальный escaping:
  "org.apache.commons" % "commons-text" % Common.Vsn.COMMONS_TEXT,

  // test
  "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % Test
    exclude("commons-logging", "commons-logging")
    exclude("org.w3c.css", "sac")
)

// После импорта настроек, typesafe-репа не кешируется. Это надо бы исправить.
resolvers ~= {
  rs => rs filter {_.name != "Typesafe Releases Repository" }
}

// Добавить резолверы, в т.ч. кэш-резолвер для отфильтрованной выше репы.
resolvers ++= {
  import Common.Repo._
  Seq(
    ("sonatype-oss-releases"   at SONATYPE_OSS_RELEASES_URL).withAllowInsecureProtocol(true),
    ("sonatype-oss-snapshots"  at SONATYPE_OSS_SNAPSHOTS_URL).withAllowInsecureProtocol(true),
    ("sonatype-groups-forge"   at SONATYPE_GROUPS_FORGE_URL).withAllowInsecureProtocol(true),
    ("apache-releases"         at APACHE_RELEASES_URL).withAllowInsecureProtocol(true),
    // brotli
    ("bintray-nitram509-jbrotli" at JBROTLI).withAllowInsecureProtocol(true),
    // Для решения проблемы с oauth2-odic-sdk:
    //("typesafe-releases"       at "http://central.maven.org/maven2").withAllowInsecureProtocol(true)
    ("typesafe-releases-art"   at TYPESAFE_RELEASES_URL).withAllowInsecureProtocol(true),
    //"jcenter"                 at JCENTER_URL
  )
}


// Импорты для play-роутера.
// play-2.8.2: CommonModelsJvm идёт первым, чтобы implicit import BindableString2 распространялся на все кастомные qsb.
// Штатный String QSB использует ненужный URLEncoder для qs-ключей, ломая ключи со скобками типа a[1].
routesImport ++= Seq(
  "io.suggest.model.CommonModelsJvm._",
  "io.suggest.routes.RoutesJvmConst._",
  "models._",
  "util.qsb._",
  "util.qsb.QSBs._",
  "models.im.ImOp._",
  "io.suggest.es.model.MEsUuId",
  "io.suggest.mbill2.m.gid.Gid_t",

  "io.suggest.adv.rcvr.RcvrKey",
  "io.suggest.n2.RcvrKeyUtil.Implicits._",
  "io.suggest.geo.GeoPoint.Implicits._",
  "io.suggest.mbill2.m.item.typ.MItemTypesJvm._",

  "io.suggest.geo.MGeoLocJvm._",
  "io.suggest.geo.MLocEnvJvm._",

  "io.suggest.n2.edge.MPredicatesJvm._",

  "io.suggest.n2.node.MNodeTypesJvm._",
  "io.suggest.url.MHostInfoJvm._",
  "io.suggest.img.MImgFmtJvm._",
  "io.suggest.compress.MCompressAlgosJvm._",
  "models.msc.MPwaManifestQsJvm._",
  "io.suggest.es.model.MEsUuIdJvm._",
  "io.suggest.dev.MScreenJvm._",
  "io.suggest.xplay.qsb.QsbSeq",
  "io.suggest.sys.mdr.MdrSearchArgsJvm._",
  "models.mext.MExtServicesJvm._",
  "io.suggest.tags.MTagsSearchQsJvm._",
  "io.suggest.captcha.MCaptchaJvm._"
)

deduplicators += { s: Seq[File] => s.headOption }

// Stylus
includeFilter in (Assets, StylusKeys.stylus) := "*.styl"

excludeFilter in (Assets, StylusKeys.stylus) := "_*.styl"


excludeFilter in digest := "*.scala"

// sbt-web
//pipelineStages ++= Seq(digest, filter, gzip)
pipelineStages ++= Seq(digest, simpleUrlUpdate, digest, filter, gzip, brotli)

excludeFilter in simpleUrlUpdate := "*.map"
//includeFilter in simpleUrlUpdate := "*.css"


testOptions in Test += Tests.Argument("-oF")


// Не генерить мусорную документацию во время stage/dist. Она не нужна никому.
sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

// org.apache.xmlbeans требует сие зависимостью. иначе proguard не пашет.
unmanagedJars in Compile ~= {uj => 
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3)+"lib/tools.jar"))) ++ uj
}

// До web21:1fe8de97fc81 включительно здесь жил конфиг для sbt-proguard. Но он так и не был доделан до рабочего состояния.
// Скорее всего, необходимо compile-time DI сначала отладить, и потом только можно будет возвращаться к этой работе.


// play-2.4: нужно устранить всякие import controllers... из шаблонов и иных мест.
routesGenerator := InjectedRoutesGenerator

// Вычистить гарантировано ненунжные ассеты. Они появляются из-за двойного вызова sbt-digest.
// У плагина sbt-filter почему-то фильтры наоборот работают. Об этом в README сказано.
// https://github.com/rgcottrell/sbt-filter
includeFilter in filter := {
  "*.md5.md5" ||
  "*.scala.*" || "*.scala" || "*.map.md5" ||
  "*.js.src.js" || "*.js.src.js.md5" ||
  "*.coffee" || "*.coffee.md5" || "*.coffee.map" ||
  "*.styl" || "*.styl.md5" ||
  "*.less" || "*.less.md5" ||
  "*-fastopt.js" || "*-fastopt.js.map" || "*-fastopt.js.md5" ||  // scala.js дебажный хлам в финальном билде не нужен.
  new PatternFilter("\\.*[a-f\\d]{32}-[a-f\\d]{32}-.+\\.[_\\w\\d.]+".r.pattern)   // Нужно фильтровать ассеты с двумя чексуммами в имени.
}


// https://www.scala-sbt.org/sbt-native-packager/formats/docker.html
dockerExposedPorts := Seq(9000)

// Используем модифицированный дистр.
dockerBaseImage := "docker-registry.suggest.io/sio/sio2/os-base-www:latest"

packageName in Docker := "docker-registry.suggest.io/sio/sio2/www"

version in Docker := "latest"

// Фиксируем директорию установки, чтобы уйти от непостоянных дефолтовых значений.
defaultLinuxInstallLocation in Docker := "/opt/sio"

// TODO Сделать одну главную папку на всё монтируемое. Там же и конфиг, и long-tmp
dockerExposedVolumes += "/opt/sio/picture"

dockerUpdateLatest := true


// Есть ассеты, которые нет смысла сжимать. Правда, они в /public, но на всякий случай сделаем.
excludeFilter in gzip := "*.woff" || "*.woff2" || "*.md5" || "*.sha1" || "*.br" || "*.apk" || "*.ipa"

excludeFilter in brotli := "*.woff" || "*.woff2" || "*.md5" || "*.sha1" || "*.gz" || "*.apk" || "*.ipa"

// Дополнительные импорты для twirl-шаблонов.
TwirlKeys.templateImports ++= Seq(
  "io.suggest.mbill2.m.gid.Gid_t",
  "play.twirl.api.HtmlFormat",
  "models.mctx.Context"
)

