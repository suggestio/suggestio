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
  val playVsn         = "2.5.1"

  /** Версия play-slick прослойки. */
  val playSlickVsn    = "2.0.0"

  /** Версия bouncy castle. */
  val bcVsn           = "1.54"

  /** Версия typesafe slick. */
  val slickVsn        = "3.1.1"

  /** Версия драйвера common-slick-driver.
   * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
  val sioSlickDrvVsn  = "0.3.0-SNAPSHOT"

  /** Версия scalatest+play. */
  val scalaTestPlusPlayVsn = "1.5.1"

  /** Версия используемого плагина play-mailer. */
  val playMailerVsn        = "5.0.0-SNAPSHOT"

}
