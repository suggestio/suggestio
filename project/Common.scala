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

    val GUICE = "4.0"

    /** Версия typesafe slick. */
    val SLICK        = "3.2.0-M2"

    /** Версия slick-pg.
      * @see [[https://github.com/tminglei/slick-pg#install]]
      */
    val SLICK_PG     = "0.15.0-M3"

    /** Версия play-slick прослойки. */
    val PLAY_SLICK   = "2.1.0-SNAPSHOT"


    object Sio {

      /** Версия драйвера common-slick-driver.
        * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
      val COMMON_SLICK_DRIVER = "0.5.0-SNAPSHOT"

    }

  }


  val SCALA_VSN = "2.11.8"
  val SCALA_VSN_JS = "2.12.1"

  val ORG = "io.suggest"

  /** Очень общие сеттинги для проектов. */
  val settingsOrg = Seq[Setting[_]](
    scalaVersion := SCALA_VSN,
    organization := ORG
  )

  val settingsOrgJS = Seq[Setting[_]](
    scalaVersion := SCALA_VSN_JS,
    organization := ORG
  )

  /** Версия play. */
  val playVsn         = "2.5.10"


  /** Версия bouncy castle. */
  def bcVsn           = "1.54"

  /** Версия scalatest+play. */
  def scalaTestPlusPlayVsn = "2.0.0-M1"

  /** Версия используемого плагина play-mailer. */
  def playMailerVsn     = "5.0.0-M1"


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
  val reactJsVsn         = "15.3.2"

  /** 
   *  Версия scalajs-react, используемая в проекте.
   *  @see [[https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md#setup]]
   */
  val reactSjsVsn        = "0.11.4-bundler-SNAPSHOT"

  /** 
   *  Версия leaflet.js. Не должна быть wildcard, потому что иначе jsDeps глючит.
   *  Где-то в leaflet-плагинах есть зависимость от wildcard-версии вида [1.0.0,),
   *  что может вызвать проблемы на сборке с пустым ivy2-кешем во время освежения версии в webjars.
   */
  val leafletJsVsn              = "1.0.2"

  /** Версия L.control.locate.js. */
  val leafletControlLocateJsVsn = "0.56.0"
  val leafletControlLocateWjVsn = leafletControlLocateJsVsn + "-1"

  /** Версия Leaflet.markercluster.js. */
  val leafletMarkerClusterJsVsn = "1.0.0"

  
  /** Версия diode.
    * @see [[https://github.com/ochrons/diode/]]
    */
  val diodeVsn = "1.1.0"

  /** Версия сериализатора boopickle. 
    * @see [[https://github.com/ochrons/boopickle]]
    */
  val boopickleVsn = "1.2.5"

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

    val APACHE_RELEASES_URL           = ARTIFACTORY_URL + "apache-releases"

    //val CONJARS_REPO_URL            = ARTIFACTORY_URL + "conjars-repo"
    //val MAVEN_TWTTR_COM_URL         = ARTIFACTORY_URL + "maven-twttr-com"

  }

}
