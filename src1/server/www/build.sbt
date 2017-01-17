//import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt.SbtTwirl
import com.typesafe.sbt.web._
import com.tuplejump.sbt.yeoman.Yeoman

Common.settingsOrg

enablePlugins(WebScalaJSBundlerPlugin)

name := "sioweb21"

version := "1.0-SNAPSHOT"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

//updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= {
 val tikaVsn       = "1.12"
 Seq(
  //jdbc exclude("com.h2database", "h2"),
  //"com.typesafe.play" %% "anorm" % "2.5.0",
  cache,
  json,
  "com.typesafe.play"   %% "play-slick" % Common.playSlickVsn,
  // slick повторно инклюдится здесь, т.к. что-то свежая версия не цеплялась через common-slick-driver
  //"com.typesafe.slick"  %% "slick"      % Common.slickVsn,
  ws exclude("commons-logging", "commons-logging"),
  "com.typesafe.play" %% "play-mailer" % Common.playMailerVsn,
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  "com.mohiva" %% "play-html-compressor" % "0.6.3",  // https://github.com/mohiva/play-html-compressor
  //"com.yahoo.platform.yui" % "yuicompressor" % "2.4.+",
  // io.suggest stuff
  Common.ORG %% "mbill2" % "0.0.0-SNAPSHOT",
  Common.ORG %% "svg-util" % "0.0.0-SNAPSHOT",
  Common.ORG %% "ipgeobase" % "0.0.0-SNAPSHOT",
  Common.ORG %% "stat" % "0.0.0-SNAPSHOT",
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
  //"com.ning" % "async-http-client" % "1.9.+",   // 2.5 migration, ahc -> 2.0.x. Удалить, если не понадобится возвращать.
  "org.slf4j" % "log4j-over-slf4j" % "1.+",
  "com.google.guava" % "guava" % "18.+",
  "com.lambdaworks" % "scrypt" % "1.4.0",     // Чтобы хешировать пароли (models.EmailPwIdent например)
  // Календарь праздников
  "de.jollyday" % "jollyday" % "0.5.+",
  "com.google.code.kaptcha" % "kaptcha" % "2.3" classifier "jdk15",
  // Драйвер postgresql 1201 потому что см. https://github.com/tminglei/slick-pg/issues/220#issuecomment-162137786
  // Нужно выпилить joda-time из проекта, перейти на java 8 datetime api, дождаться когда починят драйвер jdbc и можно будет обновляться.
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
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
  // Валидация: по идее это должно быть на уровне common, но scala-2.12 пока не пашет, оно тут:
  "com.wix"      %% "accord-core"     % Common.wixAccordVsn,
  // Поддержка JsMessages для client-side локализации сообщений
  "org.julienrf" %% "play-jsmessages" % "2.0.0",
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

  // webjars
  // карты leaflet.(js подхватывается в scala.js через jsDeps)
  "org.webjars.bower" % "leaflet"               % Common.leafletJsVsn,
  "org.webjars.bower" % "leaflet.markercluster" % Common.leafletMarkerClusterJsVsn,
  "org.webjars"       % "leaflet-locatecontrol" % Common.leafletControlLocateWjVsn,

  // Прогрессивная интеграция с js client-side:
  //"me.chrons"         %% "boopickle"            % Common.boopickleVsn, // вынесенов в [common]
  //"com.lihaoyi" %% "autowire" % "0.2.6",

  // спаривание guice и акторов требует танцев вприсядку
  //"net.codingwell" %% "scala-guice" % "4.0.0",
  "commons-io" % "commons-io" % Common.apacheCommonsIoVsn,
  // test
  "org.scalatestplus.play" %% "scalatestplus-play" % Common.scalaTestPlusPlayVsn % "test"
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
resolvers ++= {
  import Common.Repo._
  Seq(
    "typesafe-releases"       at TYPESAFE_RELEASES_URL,
    "sonatype-oss-releases"   at SONATYPE_OSS_RELEASES_URL,
    "sonatype-oss-snapshots"  at SONATYPE_OSS_SNAPSHOTS_URL,
    // kaptcha:
    "sonatype-groups-forge"   at SONATYPE_GROUPS_FORGE_URL,
    "apache-releases"         at APACHE_RELEASES_URL
  )
}



routesImport ++= Seq(
  "models._",
  "util.qsb._",
  "util.qsb.QSBs._",
  "models.im.ImOp._",
  "models.msc.AdCssArgs._",
  "io.suggest.model.es.MEsUuId",
  "io.suggest.mbill2.m.gid.Gid_t",
  "io.suggest.mbill2.m.item.typ.MItemType",
  // 2016.dec.15: GeoPoint class унифицирован на уровне [common], поэтому для его QSB теперь требуется особое приглашение:
  "io.suggest.model.geo.GeoPoint.Implicits._"
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

// До web21:1fe8de97fc81 включительно здесь жил конфиг для sbt-proguard. Но он так и не был доделан до рабочего состояния.
// Скорее всего, необходимо compile-time DI сначала отладить, и потом только можно будет возвращаться к этой работе.


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

