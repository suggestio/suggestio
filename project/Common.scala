import sbt._
import Keys._

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  /** Версия scala для сервной части и дефолтовая версия для scala.js. */
  val SCALA_VSN = "2.13.5"
  

  /** Версия scala для scala.js. */
  @inline final def SCALA_VSN_JS = SCALA_VSN

  val ORG = "io.suggest"


  /** Контейнер версий зависимостей. Вместо свалки полей в форме, записывать их надо в Vsn. */
  object Vsn {

    /** Apache Tika.
      * @see [[https://mvnrepository.com/artifact/org.apache.tika/tika-core]]
      * tika 1.25+ - требуется play-2.8.8+, иначе конфликт версий jackson
      */
    val TIKA = "1.24"

    /** Версия moment.js. */
    def momentJs = "2.24.0"

    val GUICE = "4.+"

    /** Версия typesafe slick. */
    val SLICK        = "3.3.+"

    /** Версия slick-pg.
      * @see [[https://github.com/tminglei/slick-pg#install]]
      */
    val SLICK_PG     = "0.18.+"

    /** Версия play-slick прослойки. */
    val PLAY_SLICK   = "5.+"

    /** Версия play-json. Он выведен из под основного проекта. */
    val PLAY_JSON_VSN = "2.9.+"

    val PLAY_GEOJSON = "1.6.+"


    /** Версия elasticsearch. */
    val ELASTIC_SEARCH = "5.6.8"
    //val ELASTIC_SEARCH = "6.5.4"
    // TODO 6.x Нужно объеденить остаточные модели в MNode, разъеденить оставшиеся типы по разным индексам.

    /** Бывает, что используется slf4j. Тут его версия.
      * По-хорошему, у нас на сервере logback, на клиенте -- scala-logging поверх slf4j.
      * И SLF4J депендится только в некоторых тестах.
      *
      * 2017.jul.6: Версии 1.7 и 1.8 несовместимы из-за jigsaw, нужно и logback тоже обновлять.
      */
    val SLF4J      = "1.7.25"

    /** elasticsearch и сотоварищи используют log4j impl (вместо log4j-api, ага!).
      * es-5.0 -> log4j-2.7
      * @see [[https://github.com/elastic/elasticsearch/issues/19415#issuecomment-257840940]]
      */
    val LOG4J      = "2.10.0"

    /** Версия авторских аддонов для JSR-310: threeten-extra. */
    val THREETEN_EXTRA = "1.5.+"


    // Это связано с elasticsearch-5.x. Так-то, можно уже обновлять и до 1.16.1 и 0.7 соответстветственно.
    val JTS = "1.14.0"
    val SPATIAL4J = "0.6"


    val REACT_IMAGE_GALLERY = "1.0.8"

    /** Версия scalaz.
      * Изначально добавлена в проект, чтобы наконец выкинуть вечно-кривую валидацию через wix/accord.
      * 7.3+ - требуется для monocle 1.7+
      *
      * https://github.com/scalaz/scalaz#getting-scalaz
      */
    val SCALAZ = "7.3.+"

    /** Версия scalaCSS.
      * @see [[https://japgolly.github.io/scalacss/book/quickstart/index.html]]
      */
    val SCALACSS = "0.7.0"

    /** Улучшенное жестко-типизированное сравнение.
      * 1.3.x требует scalacss 0.7.0
      * @see [[https://github.com/japgolly/univeq#scalaz]]
      */
    val UNIVEQ = "1.3.+"

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
    val MACWIRE = "2.3.+"

    /** Версия wysiwyg-редактора quill, который будем пытаться использовать заместо tinymce.
      * @see [[https://github.com/quilljs/quill]]
      */
    // TODO Кажется, эти значения ни на что не влияют. react-quill тащит свою версию quill с собой.
    val QUILL = "1.3.7"
    val QUILL_DELTA = "4.2.2"

    /** Версия react-quill
      * @see [[https://www.npmjs.com/package/react-quill]]
      */
    val REACT_QUILL = "1.3.5"

    /** @see [[https://github.com/PaulLeCam/react-leaflet]]
      * @see [[https://www.npmjs.com/package/react-leaflet]]
      */
    val REACT_LEAFLET             = "3.1.0"

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
    val STRING_REPLACE_LOADER_JS = "2.1.1"

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
    val AKKA = "2.6.10"

    /** Версия akka-contrib-extra. Вероятно, будет нужна для реактивного i/o из шелла.
      * @see [[https://github.com/typesafehub/akka-contrib-extra]]
      */
    val AKKA_CONTRIB_EXTRA = "4.+"

    /** Версия jBrotli-биндингов.
      * @see [[https://github.com/MeteoGroup/jbrotli]]
      */
    val JBROTLI = "0.5.0"

    /** Версия react-scroll для управления скроллингом.
      * @see [[https://github.com/fisshy/react-scroll]]
      */
    val REACT_SCROLL = "1.8.1"

    /** Упрощённое API-обёртка над WebBluetooth для работы с маячками eddystone прямо из браузера.
      * @see [[https://github.com/zurfyx/eddystone-web-bluetooth]]
      */
    val EDDYSTONE_WEB_BLUETOOTH_JS = "1.0.1"

    /** ServiceWorker toolbox.
      * @see [[https://www.npmjs.com/package/sw-toolbox]]
      */
    val SW_TOOLBOX = "3.6.0"

    /** Версия monocle.
      * 1.7.x - scalaz 7.3 и выше.
      * 2.x   - переезд на cats, это параллель для scalaz, на которую завязана целая кода.
      * @see [[http://julien-truffaut.github.io/Monocle/]]
      */
    val MONOCLE = "1.7.+"

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
    val SCALA_JAVA_TIME = "2.1.0"

    /** Flow.js library providing multiple simultaneous, stable and resumable uploads via the HTML5 File API.
      * @see [[https://github.com/flowjs/flow.js/]]
      */
    val FLOW_JS = "2.14.1"


    val MATERIAL_UI = "5.0.0-alpha.30"
    val MATERIAL_UI_ICONS = "5.0.0-alpha.28"

    /** @see [[https://www.npmjs.com/package/@emotion/react]] */
    val EMOTION_REACT = "11.1.5"
    /** @see [[https://www.npmjs.com/package/@emotion/styled]] */
    def EMOTION_STYLED = EMOTION_REACT

    /** The lightest colorpicker for React Material-Ui.
      * @see [[https://github.com/mikbry/material-ui-color]]
      */
    val MATERIALUI_COLOR = "1.1.0"

    /** material-ui-color зачем-то зависит от styled-components. */
    val STYLED_COMPONENTS = "5.2.2"

  }


  val settingsBase = Seq[Setting[_]](
    organization := ORG,
    // Выключение сборки документации
    //sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
    // Ускорение резолва зависимостей путём запрета их резолва без явной необходимости.
    //, offline := true
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
  def scalaTestPlusPlayVsn = "5.0.0"

  /** Версия используемого плагина play-mailer. */
  def playMailerVsn     = "8.0.0"


  /** Версия jquery-фасада для scalajs.
   * @see [[https://github.com/scala-js/scala-js-jquery]]
   */
  def sjsJqueryVsn      = "1.0.+"

  /** Версия scalajs-dom.
   * @see [[https://github.com/scala-js/scala-js-dom]]
   */
  def sjsDomVsn         = "1.+"

  /** Версия Apache commons-io. */
  def apacheCommonsIoVsn = "2.6"

  /** Версия scalatest. */
  def scalaTestVsn       = "3.1.1"

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
  val diodeReactVsn = diodeVsn //+ "." + reactSjsVsn.replaceAllLiterally(".", "")

  /** Версия enumeratum, версия нарисована прямо в заголовке на maven badge.
    * @see [[https://github.com/lloydmeta/enumeratum#enumeratum------]]
    */
  val enumeratumVsn = "1.6.+"

  /** Версия minitest, используемого для простых кросс-платформенных тестов.
    * @see [[https://github.com/monix/minitest]]
    */
  val minitestVsn  = "2.8.+"


  object Repo {

    /** Адрес внутреннего кеширующего сервера artifactory */
    val ARTIFACTORY_URL               = "http://ci.suggest.io/artifactory/"

    val TYPESAFE_RELEASES_URL         = ARTIFACTORY_URL + "typesafe-releases"

    val SONATYPE_OSS_RELEASES_URL     = ARTIFACTORY_URL + "sonatype-oss-releases"
    val SONATYPE_OSS_SNAPSHOTS_URL    = ARTIFACTORY_URL + "sonatype-oss-snapshots"
    val SONATYPE_GROUPS_FORGE_URL     = ARTIFACTORY_URL + "sonatype-groups-forge"

    def APACHE_RELEASES_URL           = ARTIFACTORY_URL + "apache-releases"

    def JCENTER_URL                   = ARTIFACTORY_URL + "jcenter"

    val JROPER_MAVEN_REPO             = ARTIFACTORY_URL + "jroper-maven"
    def JBROTLI                       = ARTIFACTORY_URL + "bintray-nitram509-jbrotli"

    //val CONJARS_REPO_URL            = ARTIFACTORY_URL + "conjars-repo"
    //val MAVEN_TWTTR_COM_URL         = ARTIFACTORY_URL + "maven-twttr-com"

  }

}
