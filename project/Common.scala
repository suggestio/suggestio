import sbt._
import Keys._

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  /** Версия scala для сервной части и дефолтовая версия для scala.js. */
  val SCALA_VSN = "2.12.4"

  /** Версия scala для scala.js. */
  @inline final def SCALA_VSN_JS = SCALA_VSN

  val ORG = "io.suggest"


  /** Контейнер версий зависимостей. Вместо свалки полей в форме, записывать их надо в Vsn. */
  object Vsn {

    /** Версия moment.js. */
    def momentJs = "2.17.1"

    val GUICE = "4.1.+"

    /** Версия typesafe slick. */
    val SLICK        = "3.2.+"

    /** Версия slick-pg.
      * @see [[https://github.com/tminglei/slick-pg#install]]
      */
    val SLICK_PG     = "0.15.0-RC"

    /** Версия play-slick прослойки. */
    val PLAY_SLICK   = "3.0.+"

    /** Версия play-json. Он выведен из под основного проекта. */
    val PLAY_JSON_VSN = "2.6.+"

    val PLAY_GEOJSON = "1.5.+"

    /** Версия yandex-money-sdk-java.
     *  @see [[https://github.com/yandex-money/yandex-money-sdk-java#gradle-dependency-jcenter]]
     */
    //val YA_MONEY_SDK = "6.1.+"

    /** Версия elasticsearch. */
    val ELASTIC_SEARCH = "5.4.2"

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
    val LOG4J      = "2.7"

    /** Версия авторских аддонов для JSR-310: threeten-extra. */
    val THREETEN_EXTRA = "1.0"

    val JTS = "1.14.0"
    val SPATIAL4J = "0.6"

    object Sio {

      /** Версия драйвера common-slick-driver.
        * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
      val COMMON_SLICK_DRIVER = "1.0.2-SNAPSHOT"

    }

    val REACT_IMAGE_GALLERY = "0.7.15"

    /** Версия play-ws, который уехал за пределы play начиная с play-2.6.0.
      * @see [[https://playframework.com/documentation/2.6.x/WSMigration26]]
      *      [[https://github.com/playframework/play-ws]]
      */
    val PLAY_WS = "1.0.0"

    /** Версия scalaz.
      * Изначально добавлена в проект, чтобы наконец выкинуть вечно-кривую валидацию через wix/accord.
      *
      * https://github.com/scalaz/scalaz#getting-scalaz
      */
    val SCALAZ = "7.2.14"

    /** Версия scalaCSS. Изначально появилась в sc-v3 для замены около-динамических стилей,
      * геморройно подгружаемых с сервера.
      *
      * @see [[https://japgolly.github.io/scalacss/book/quickstart/index.html]]
      */
    // TODO обновится на основную ветку. когда добавят/замёржат fontFamily(String)
    val SCALACSS = "0.5.3"

    /** Улучшенное жестко-типизированное сравнение.
      * @see [[https://github.com/japgolly/univeq#scalaz]]
      */
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
    val MACWIRE = "2.3.0"

    /** @see [[https://www.npmjs.com/package/react-tinymce]] */
    val REACT_TINYMCE = "0.6.0"

    /** react-tinymce неявно зависит от tinymce, который должен бы быть в global scope.
      *
      * @see [[https://github.com/instructure-react/react-tinymce#dependency]]
      */
    val TINYMCE = "4.2"


    /** Версия wysiwyg-редактора quill, который будем пытаться использовать заместо tinymce.
      * @see [[https://github.com/quilljs/quill]]
      */
    val QUILL = "1.3.2"
    val QUILL_DELTA = "3.6.0"

    /** Версия react-quill
      * @see [[https://www.npmjs.com/package/react-quill]]
      */
    val REACT_QUILL = "1.1.0"

    val REACT_LEAFLET = "1.6.6"

    /** Окостыливание экспортов js-модулей для совместимости с webpack и es-modules.
      * @see [[https://github.com/webpack-contrib/exports-loader]]
      * @see [[https://www.npmjs.com/package/exports-loader]]
      */
    val EXPORTS_LOADER_JS = "0.6.4"

    /** Окостыливание импортов js-модулей для совместимости с webpack.
      * @see [[https://github.com/webpack-contrib/imports-loader]]
      */
    val IMPORTS_LOADER_JS = "0.7.1"

    /** AsmCrypto.js hi-speed crypto routines.
      *
      * @see [[https://github.com/vibornoff/asmcrypto.js]]
      * @see [[https://www.npmjs.com/package/asmcrypto.js]]
      */
    val ASM_CRYPTO_JS = "0.0.11"

    /** Версия Apache Batik для работы с SVG.
      * @see [[https://xmlgraphics.apache.org/batik/download.html]]
      */
    val APACHE_BATIK = "1.9"

  }


  val settingsBase = Seq[Setting[_]](
    organization := ORG,
    // Выключение сборки документации
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    // Ускорение резолва зависимостей путём запрета их резолва без явной необходимости.
    offline := true
  )

  /** Очень общие сеттинги для jvm-проектов. */
  val settingsOrg = settingsBase ++ Seq[Setting[_]](
    scalaVersion := SCALA_VSN
  )

  /** Очень общие сеттинги для js-проектов. */
  val settingsOrgJS = settingsBase ++ Seq[Setting[_]](
    scalaVersion := SCALA_VSN_JS,
    // scala.js [0.6.18 .. 1.0) Включить @ScalaJSDefined по умолчанию.
    scalacOptions += "-P:scalajs:sjsDefinedByDefault"
  )

  /** Версия play. */
  val playVsn         = "2.6.5"


  /** Версия bouncy castle. */
  def bcVsn           = "1.56"

  /** Версия scalatest+play. */
  def scalaTestPlusPlayVsn = "3.0.0"

  /** Версия используемого плагина play-mailer. */
  def playMailerVsn     = "6.0.0"


  /** Версия jquery-фасада для scalajs.
   * @see [[https://github.com/scala-js/scala-js-jquery]]
   */
  def sjsJqueryVsn      = "0.9.1"

  /** Версия scalajs-dom.
   * @see [[https://github.com/scala-js/scala-js-dom]]
   */
  def sjsDomVsn         = "0.9.3"

  /** Версия Apache commons-io. */
  def apacheCommonsIoVsn = "2.4"

  /** Версия scalatest. */
  def scalaTestVsn       = "3.0.1"

  /** 
   *  Версия react.js, используемая в проекте. 
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  val reactJsVsn         = "15.6.1"

  /** 
   *  Версия scalajs-react, используемая в проекте.
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  val reactSjsVsn        = "1.1.0"

  /** 
   *  Версия leaflet.js. Не должна быть wildcard, потому что иначе jsDeps глючит.
   *  Где-то в leaflet-плагинах есть зависимость от wildcard-версии вида [1.0.0,),
   *  что может вызвать проблемы на сборке с пустым ivy2-кешем во время освежения версии в webjars.
   */
  val leafletJsVsn              = "1.2.0"

  /** Версия L.control.locate.js. */
  val leafletControlLocateJsVsn = "0.62.0"

  /** Версия Leaflet.markercluster.js. */
  val leafletMarkerClusterJsVsn = "1.1.0"

  
  /** Версия diode.
    * @see [[https://github.com/ochrons/diode/]]
    */
  val diodeVsn = "1.1.2"

  /** Версия сериализатора boopickle. 
    * @see [[https://github.com/ochrons/boopickle]]
    */
  val boopickleVsn = "1.2.6"

  /** Версия enumeratum, версия нарисована прямо в заголовке на maven badge.
    * @see [[https://github.com/lloydmeta/enumeratum#enumeratum------]]
    */
  val enumeratumVsn = "1.5.+"

  /** Версия minitest, используемого для простых кросс-платформенных тестов.
    * @see [[https://github.com/monix/minitest]]
    */
  val minitestVsn  = "1.+"


  object Repo {

    /** Адрес внутреннего кеширующего сервера artifactory */
    val ARTIFACTORY_URL               = "http://ivy2-internal.cbca.ru/artifactory/"
    //val ARTIFACTORY_URL             = "http://10.0.0.254:8081/artifactory/"

    val TYPESAFE_RELEASES_URL         = ARTIFACTORY_URL + "typesafe-releases"

    val SONATYPE_OSS_RELEASES_URL     = ARTIFACTORY_URL + "sonatype-oss-releases"
    val SONATYPE_OSS_SNAPSHOTS_URL    = ARTIFACTORY_URL + "sonatype-oss-snapshots"
    val SONATYPE_GROUPS_FORGE_URL     = ARTIFACTORY_URL + "sonatype-groups-forge"

    def APACHE_RELEASES_URL           = ARTIFACTORY_URL + "apache-releases"

    def JCENTER_URL                   = ARTIFACTORY_URL + "jcenter"

    val JROPER_MAVEN_REPO             = ARTIFACTORY_URL + "jroper-maven"

    //val CONJARS_REPO_URL            = ARTIFACTORY_URL + "conjars-repo"
    //val MAVEN_TWTTR_COM_URL         = ARTIFACTORY_URL + "maven-twttr-com"

  }

}
