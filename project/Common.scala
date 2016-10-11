import sbt._
import Keys._

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  val SCALA_VSN = "2.11.8"

  val ORG = "io.suggest"

  /** Очень общие сеттинги для проектов. */
  val settingsOrg = Seq[Setting[_]](
    scalaVersion := SCALA_VSN,
    organization := ORG
  )

  /** Версия play. */
  val playVsn         = "2.5.9"

  /** Версия play-slick прослойки. */
  def playSlickVsn    = "2.0.2"

  /** Версия bouncy castle. */
  def bcVsn           = "1.54"

  /** Версия typesafe slick. */
  def slickVsn        = "3.1.1"

  /** Версия драйвера common-slick-driver.
   * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
  def sioSlickDrvVsn  = "0.3.0-SNAPSHOT"

  /** Версия scalatest+play. */
  def scalaTestPlusPlayVsn = "1.5.0"

  /** Версия используемого плагина play-mailer. */
  def playMailerVsn     = "5.0.0-M1"


  /** Версия jquery-фасада для scalajs.
   * @see [[https://github.com/scala-js/scala-js-jquery]]
   */
  def sjsJqueryVsn      = "0.9.0"

  /** Версия scalajs-dom.
   * @see [[https://github.com/scala-js/scala-js-dom]]
   */
  def sjsDomVsn         = "0.9.0"

  /** Версия Apache commons-io. */
  def apacheCommonsIoVsn = "2.4"

  /** Версия scalatest.
    * play-2.2..2.5 == scalatest-2.2.x
    */
  def scalaTestVsn       = "2.2.+"

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
