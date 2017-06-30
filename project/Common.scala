import sbt._
import Keys._

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  /** Контейнер версий зависимостей. Вместо свалки полей в форме, записывать их надо в Vsn. */
  object Vsn {

    /** Версия moment.js. */
    def momentJs = "2.17.1"

    val GUICE = "4.1.0"

    /** Версия typesafe slick. */
    val SLICK        = "3.2.0"

    /** Версия slick-pg.
      * @see [[https://github.com/tminglei/slick-pg#install]]
      */
    val SLICK_PG     = "0.15.0-RC"

    /** Версия play-slick прослойки. */
    val PLAY_SLICK   = "3.0.0"

    /** Версия play-json. Он выведен из под основного проекта. */
    val PLAY_JSON_VSN = "2.6.0"

    /** Версия yandex-money-sdk-java.
     *  @see [[https://github.com/yandex-money/yandex-money-sdk-java#gradle-dependency-jcenter]]
     */
    //val YA_MONEY_SDK = "6.1.+"

    /** Версия elasticsearch. */
    val ELASTIC_SEARCH = "5.4.2"

    /** Бывает, что используется slf4j. Тут его версия.
      * По-хорошему, у нас на сервере logback, на клиенте -- самописный логгинг.
      * И SLF4J депендится только в некоторых тестах.
      */
    val SLF4J      = "1.7.+"

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

  }


  val SCALA_VSN = "2.11.8"
  val SCALA_VSN_JS = "2.12.1"

  val ORG = "io.suggest"

  /** Очень общие сеттинги для проектов. */
  val settingsOrg = Seq[Setting[_]](
    scalaVersion := SCALA_VSN,
    organization := ORG,
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )

  val settingsOrgJS = Seq[Setting[_]](
    scalaVersion := SCALA_VSN_JS,
    organization := ORG,
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )

  /** Версия play. */
  val playVsn         = "2.6.0"


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
  def sjsDomVsn         = "0.9.1"

  /** Версия Apache commons-io. */
  def apacheCommonsIoVsn = "2.4"

  /** Версия scalatest. */
  def scalaTestVsn       = "3.0.1"

  /** 
   *  Версия react.js, используемая в проекте. 
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  //val reactJsVsn         = "15.3.2"
  val reactJsVsn         = "15.5.4"

  /** 
   *  Версия scalajs-react, используемая в проекте.
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  //val reactSjsVsn        = "0.11.4-bundler-SNAPSHOT"
  val reactSjsVsn        = "1.0.1"

  /** 
   *  Версия leaflet.js. Не должна быть wildcard, потому что иначе jsDeps глючит.
   *  Где-то в leaflet-плагинах есть зависимость от wildcard-версии вида [1.0.0,),
   *  что может вызвать проблемы на сборке с пустым ivy2-кешем во время освежения версии в webjars.
   */
  val leafletJsVsn              = "1.0.3"

  /** Версия L.control.locate.js. */
  val leafletControlLocateJsVsn = "0.61.0"
  val leafletControlLocateWjVsn = leafletControlLocateJsVsn + "-1"

  /** Версия Leaflet.markercluster.js. */
  val leafletMarkerClusterJsVsn = "1.0.5"

  
  /** Версия diode.
    * @see [[https://github.com/ochrons/diode/]]
    */
  val diodeVsn = "1.1.2"

  /** Версия сериализатора boopickle. 
    * @see [[https://github.com/ochrons/boopickle]]
    */
  val boopickleVsn = "1.2.6"

  /** Версия autowire для клиента и сервера.
    *  @see [[https://github.com/lihaoyi/autowire]]
    */
  //val autowireVsn  = "0.2.6"

  /** Версия enumeratum, версия нарисована прямо в заголовке на maven badge.
    * @see [[https://github.com/lloydmeta/enumeratum#enumeratum------]]
    */
  val enumeratumVsn = "1.5.3"

  /** Версия wix accord validation.
    * @see [[http://wix.github.io/accord/index.html#getting-started]]
    * 
    * v[,0.6.1] имеют серьезную проблему: https://github.com/wix/accord/issues/97
    */
  val wixAccordVsn = "0.7-SNAPSHOT"

  /** Версия minitest, используемого для простых кросс-платформенных тестов.
    * @see [[https://github.com/monix/minitest]]
    */
  val minitestVsn  = "0.27"


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

    //val CONJARS_REPO_URL            = ARTIFACTORY_URL + "conjars-repo"
    //val MAVEN_TWTTR_COM_URL         = ARTIFACTORY_URL + "maven-twttr-com"

  }

}
