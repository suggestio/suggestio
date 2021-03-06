import sbt._
import Keys._
import org.scalajs.linker.interface.ESVersion

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  /** Версия scala для сервной части и дефолтовая версия для scala.js. */
  val SCALA_VSN = "2.13.7"
  

  /** Версия scala для scala.js. */
  @inline final def SCALA_VSN_JS = SCALA_VSN

  val ORG = "io.suggest"


  /** Контейнер версий зависимостей. Вместо свалки полей в форме, записывать их надо в Vsn. */
  object Vsn {

    /** Output ECMAScript version. */
    final def ECMA_SCRIPT = ESVersion.ES2019

    /** Apache Tika.
      * @see [[https://mvnrepository.com/artifact/org.apache.tika/tika-core]]
      * tika 1.25+ - need play-2.8.8++, overwise conflicting jackson-databind versions with akka right after [www] run.
      */
    val TIKA = "1.24"

    /** Версия moment.js. */
    def momentJs = "2.24.0"

    val GUICE = "4.2.3"

    /** Typesafe slick version.
      */
    val SLICK        = "3.3.+"

    /** slick-pg version.
      * @see [[https://github.com/tminglei/slick-pg#install]]
      */
    val SLICK_PG     = "0.19.+"

    /** Версия play-slick прослойки. */
    val PLAY_SLICK   = "5.+"

    /** Версия play-json. Он выведен из под основного проекта. */
    val PLAY_JSON_VSN = "2.9.+"

    val PLAY_GEOJSON = "1.7.+"


    /** Версия elasticsearch. */
    val ELASTIC_SEARCH = "7.16.1"
    val JTS = "1.15.0"
    val SPATIAL4J = "0.7"

    /** Бывает, что используется slf4j. Тут его версия.
      * По-хорошему, у нас на сервере logback, на клиенте -- scala-logging поверх slf4j.
      * И SLF4J депендится только в некоторых тестах.
      *
      * 2017.jul.6: Версии 1.7 и 1.8 несовместимы из-за jigsaw, нужно и logback тоже обновлять.
      * 2021-12-10: Logback 1.2.3 in classpath can work only with slf4j-* 1.7.x. Need play-framework update >2.9.
      */
    val SLF4J      = "1.7.+"

    /** elasticsearch и сотоварищи используют log4j impl (вместо log4j-api, ага!).
      * es-5.0 -> log4j-2.7
      * @see [[https://github.com/elastic/elasticsearch/issues/19415#issuecomment-257840940]]
      */
    val LOG4J      = "2.+"

    /** Версия авторских аддонов для JSR-310: threeten-extra. */
    val THREETEN_EXTRA = "1.5.+"


    val REACT_IMAGE_GALLERY = "1.0.8"

    /** Версия scalaz.
      * Изначально добавлена в проект, чтобы наконец выкинуть вечно-кривую валидацию через wix/accord.
      * 7.3+ - требуется для monocle 1.7+
      *
      * https://github.com/scalaz/scalaz#getting-scalaz
      */
    val SCALAZ = "7.3.+"

    /** ScalaCSS ABI is connected with UnivEq ABI.
      * @see [[https://japgolly.github.io/scalacss/book/quickstart/index.html]]
      */
    val SCALACSS = "0.8.0-RC1"

    /** @see [[https://github.com/japgolly/univeq#scalaz]] */
    val UNIVEQ = "1.+"

    /** Apache commons lang3
      * @see [[https://commons.apache.org/proper/commons-lang/]]
      */
    val COMMONS_LANG3 = "3.+"

    /** Apache commons text
      * @see [[https://commons.apache.org/proper/commons-text/]]
      */
    val COMMONS_TEXT  = "1.+"

    /** MacWire -- scala compile-time DI framework.
      *
      * @see [[https://github.com/adamw/macwire#installation-using-with-sbt]]
     */
    val MACWIRE = "2.5.0"

    /** Версия wysiwyg-редактора quill, который будем пытаться использовать заместо tinymce.
      * @see [[https://www.npmjs.com/package/quill?activeTab=versions]]
      * @see [[https://github.com/quilljs/quill]]
      */
    // TODO Кажется, эти значения ни на что не влияют. react-quill тащит свою версию quill с собой.
    val QUILL = "2.0.0-dev.4"
    val QUILL_DELTA = "4.2.2"

    /** Версия react-quill
      * @see [[https://www.npmjs.com/package/react-quill]]
      */
    val REACT_QUILL = "2.0.0-beta.4"

    /** @see [[https://www.npmjs.com/package/jodit-react]] */
    val JODIT_REACT = "1.0.83"
    /** @see [[https://www.npmjs.com/package/jodit]] */
    val JODIT = "3.6.11"

    /** @see [[https://github.com/PaulLeCam/react-leaflet]]
      * @see [[https://www.npmjs.com/package/react-leaflet]]
      */
    val REACT_LEAFLET             = "3.2.2"

    /** Окостыливание экспортов js-модулей для совместимости с webpack и es-modules.
      * @see [[https://github.com/webpack-contrib/exports-loader]]
      * @see [[https://www.npmjs.com/package/exports-loader]]
      */
    val EXPORTS_LOADER_JS = "0.7.0"

    /** Окостыливание импортов js-модулей для совместимости с webpack.
      * @see [[https://github.com/webpack-contrib/imports-loader]]
      */
    val IMPORTS_LOADER_JS = "0.8.0"

    /** Замена кода в js-файлах при сборке.
      * @see [[https://www.npmjs.com/package/string-replace-loader]]
      */
    val STRING_REPLACE_LOADER_JS = "2.3.0"

    /** AsmCrypto.js hi-speed crypto routines.
      *
      * @see [[https://github.com/asmcrypto/asmcrypto.js]]
      * @see [[https://www.npmjs.com/package/asmcrypto.js]]
      */
    // TODO "2.3.2" или выше. API сменилось на java-подобное.
    val ASM_CRYPTO_JS = "0.0.11"

    /** Версия Apache Batik для работы с SVG.
      * @see [[https://xmlgraphics.apache.org/batik/download.html]]
      */
    val APACHE_BATIK = "1.+"

    /** Версия scala-parser-combinators.
      * @see [[https://github.com/scala/scala-parser-combinators]]
      */
    val SCALA_PARSER_COMBINATORS = "1.+"

    /** Версия react-grid-layout.
      * @see [[https://github.com/strml/react-grid-layout]]
      */
    val REACT_GRID_LAYOUT = "0.18.2"

    /** Версия react-stonecutter для организации плитки.
      * @see [[https://github.com/dantrain/react-stonecutter]]
      */
    val REACT_STONECUTTER = "0.3.10"

    /** Версия аккордеона react-sanfona.
      * @see [[https://github.com/daviferreira/react-sanfona]]
      */
    val REACT_SANFONA = "1.2.0"

    /** Версия react-sidebar для быстрой организации боковых панелек.
      * @see [[https://github.com/balloob/react-sidebar]]
      */
    val REACT_SIDEBAR = "3.0.2"

    /** Версия STRML/react-resizable.
      * @see [[https://github.com/STRML/react-resizable]]
      */
    val REACT_RESIZABLE = "1.11.0"

    /** Версия pathikrit/better-files для удобной работы с файлами.
      * @see [[https://github.com/pathikrit/better-files]]
      */
    //val BETTER_FILES = "3.8.0"

    /** Используемая в проекте версия akka. */
    val AKKA = "2.6.14"

    /** Версия jBrotli-биндингов.
      * @see [[https://github.com/MeteoGroup/jbrotli]]
      */
    val JBROTLI = "0.5.0"

    /** Версия react-scroll для управления скроллингом.
      * @see [[https://github.com/fisshy/react-scroll]]
      */
    val REACT_SCROLL = "1.8.4"

    /** Упрощённое API-обёртка над WebBluetooth для работы с маячками eddystone прямо из браузера.
      * @see [[https://github.com/zurfyx/eddystone-web-bluetooth]]
      */
    val EDDYSTONE_WEB_BLUETOOTH_JS = "1.0.1"

    /** ServiceWorker toolbox.
      * @see [[https://www.npmjs.com/package/sw-toolbox]]
      */
    val SW_TOOLBOX = "3.6.0"

    /** @see [[http://julien-truffaut.github.io/Monocle/]] */
    val MONOCLE = "3.+"

    /** @see [[https://mvnrepository.com/artifact/commons-codec/commons-codec]] */
    val APACHE_COMMONS_CODEC = "1.12"

    val REACT_MEASURE = "2.5.2"

    /** @see [[https://www.npmjs.com/package/react-dnd]]. */
    val REACT_DND = "11.1.3"

    /** @see [[https://www.npmjs.com/package/qrcode.react]]
      * @see [[https://github.com/zpao/qrcode.react]]
      */
    val REACT_QRCODE = "1.0.1"

    /** java.time.* API in scala
      * @see [[https://github.com/cquiroz/scala-java-time]]
      * @see [[https://mvnrepository.com/artifact/io.github.cquiroz/scala-java-time]]
      */
    val SCALA_JAVA_TIME = "2.+"

    /** Flow.js library providing multiple simultaneous, stable and resumable uploads via the HTML5 File API.
      * @see [[https://github.com/flowjs/flow.js/]]
      */
    val FLOW_JS = "2.14.1"


    // TODO After 5.0.0-rc.0 packages has been renamed to @mui/*. https://github.com/mui-org/material-ui/blob/next/CHANGELOG.md#500-rc0
    //      Mui dependencies (color, treasury) need attention before mui update.
    val MATERIAL_UI = "5.0.0-beta.5"
    val MATERIAL_UI_ICONS = "5.0.0-beta.5"
    val MATERIAL_UI_LAB = "5.0.0-alpha.44"

    /** @see [[https://www.npmjs.com/package/@emotion/react]] */
    val EMOTION_REACT = "11.6.0"
    /** @see [[https://www.npmjs.com/package/@emotion/styled]] */
    val EMOTION_STYLED = "11.6.0"

    /** The lightest colorpicker for React Material-Ui.
      * @see [[https://github.com/mikbry/material-ui-color]]
      */
    val MATERIALUI_COLOR = "1.2.0"

    /** @see [[https://github.com/siriwatknp/mui-treasury]] */
    val MUI_TREASURY = "2.0.0-alpha.1"

    /** @see [[https://github.com/OWASP/java-html-sanitizer/releases]] */
    val OWASP_JAVA_HTML_SANITIZER = "20211018.2"

    /** @see [[https://github.com/japgolly/scala-graal]] */
    val SCALA_GRAAL = "2.0.0"

    /** @see [[https://github.com/typelevel/cats-effect#getting-started]] */
    val CATS_EFFECT = "3.+"

    /** @see [[https://github.com/typelevel/cats]] */
    val CATS = "2.+"

  }


  val settingsBase = Seq[Setting[_]](
    organization := ORG,
    // Выключение сборки документации
    //sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    // Ускорение резолва зависимостей путём запрета их резолва без явной необходимости.
    //, offline := true
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  )

  /** Очень общие сеттинги для jvm-проектов. */
  val settingsOrg = settingsBase ++ Seq[Setting[_]](
    scalaVersion := SCALA_VSN,
  )

  /** Очень общие сеттинги для js-проектов. */
  val settingsOrgJS = settingsBase ++ Seq[Setting[_]](
    scalaVersion := SCALA_VSN_JS
  )

  /** Версия play. */
  val playVsn         = "2.8.8"


  /** Версия BouncyCastle. */
  def bcVsn           = "1.65"

  /** Версия scalatest+play. */
  def scalaTestPlusPlayVsn = "5.+"

  /** Версия используемого плагина play-mailer. */
  def playMailerVsn     = "8.+"


  /** Версия jquery-фасада для scalajs.
   * @see [[https://github.com/scala-js/scala-js-jquery]]
   */
  def sjsJqueryVsn      = "1.0.+"

  /** Версия scalajs-dom.
   * @see [[https://github.com/scala-js/scala-js-dom]]
   */
  def sjsDomVsn         = "1.+" // TODO v2.+ => + react + diode + univeq + etc

  /** Версия Apache commons-io. */
  def apacheCommonsIoVsn = "2.6"

  /** Версия scalatest. */
  def scalaTestVsn       = "3.2.+"

  /** 
   *  Версия react.js, используемая в проекте. 
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  val reactJsVsn         = "17.0.2"

  /** 
   *  Версия scalajs-react, используемая в проекте.
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  val reactSjsVsn        = "1.7.7" // И контроллировать суффикс diodeReactVsn ниже!
  // TODO reactSjsVsn "2.+"

  /** 
   *  Версия leaflet.js. Не должна быть wildcard, потому что иначе jsDeps глючит.
   *  Где-то в leaflet-плагинах есть зависимость от wildcard-версии вида [1.0.0,),
   *  что может вызвать проблемы на сборке с пустым ivy2-кешем во время освежения версии в webjars.
   *  1.3.2 - Первая публичная версия без window.L вообще по дефолту. 1.3.3 - временный rollback.
   */
  val leafletJsVsn              = "1.7.1"

  /** Версия L.control.locate.js.
    * 0.62.0: грязно проверяет и работает с window.L, что конфликтует minified js. string-replace решает проблему. */
  val leafletControlLocateJsVsn = "0.72.0"

  /** Версия Leaflet.markercluster.js.
    * @see [[https://github.com/Glartek/Leaflet.markercluster]]
    * @see [[https://www.npmjs.com/package/@glartek/leaflet.markercluster]]
    */
  val leafletMarkerClusterJsVsn = "1.4.4"

  
  /** Версия diode.
    * @see [[https://github.com/ochrons/diode/]]
    */
  val diodeVsn = "1.1.+"
  //val diodeVsn = "1.2.+" // TODO
  val diodeReactVsn = diodeVsn //+ "." + reactSjsVsn.replaceAllLiterally(".", "")

  /** Версия enumeratum, версия нарисована прямо в заголовке на maven badge.
    * @see [[https://github.com/lloydmeta/enumeratum#enumeratum------]]
    */
  val enumeratumVsn = "1.6.+"

  /** Версия minitest, используемого для простых кросс-платформенных тестов.
    * @see [[https://github.com/monix/minitest]]
    */
  val minitestVsn  = "2.+"


  object Repo {

    val TYPESAFE_RELEASES_URL         = "https://repo.typesafe.com/typesafe/releases/"

    val SONATYPE_OSS_RELEASES_URL     = "https://oss.sonatype.org/content/repositories/releases/"
    val SONATYPE_OSS_SNAPSHOTS_URL    = "https://oss.sonatype.org/content/repositories/snapshots/"
    val SONATYPE_GROUPS_FORGE_URL     = "https://repository.sonatype.org/content/groups/forge/"

    def APACHE_RELEASES_URL           = "https://repository.apache.org/content/repositories/releases/"

    val JROPER_MAVEN_REPO             = "https://dl.bintray.com/jroper/maven/"

    // jbrotli: "https://dl.bintray.com/nitram509/jbrotli" => 403
    def JBROTLI                       = "http://ci.suggest.io/artifactory/bintray-nitram509-jbrotli"

  }

}
