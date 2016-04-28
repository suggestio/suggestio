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
  val playVsn         = "2.5.2"

  /** Версия play-slick прослойки. */
  val playSlickVsn    = "2.0.2"

  /** Версия bouncy castle. */
  val bcVsn           = "1.54"

  /** Версия typesafe slick. */
  val slickVsn        = "3.1.1"

  /** Версия драйвера common-slick-driver.
   * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
  val sioSlickDrvVsn  = "0.3.0-SNAPSHOT"

  /** Версия scalatest+play. */
  val scalaTestPlusPlayVsn = "1.5.0"

  /** Версия используемого плагина play-mailer. */
  val playMailerVsn        = "5.0.0-M1"


  /** Версия jquery-фасада для scalajs.
   * @see [[https://github.com/scala-js/scala-js-jquery]]
   */
  val sjsJqueryVsn     = "0.9.0"

  /** Версия scalajs-dom.
   * @see [[https://github.com/scala-js/scala-js-dom]]
   */
  val sjsDomVsn        = "0.9.0"
}
