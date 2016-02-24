import sbt._
import Keys._

/**
 * Очень общие сеттинги для под-проектов.
 * Очень краткий и понятный мануал по ссылке ниже.
 * @see [[https://www.playframework.com/documentation/2.5.x/SBTSubProjects#Sharing-common-variables-and-code]]
 */

object Common {

  val SCALA_VSN = "2.11.7"

  val ORG = "io.suggest"

  /** Очень общие сеттинги для проектов. */
  val settingsOrg = Seq[Setting[_]](
    scalaVersion := SCALA_VSN,
    organization := ORG
  )

  /** Версия play. */
  val playVsn         = "2.4.6"

  /** Версия play-slick прослойки. */
  // TODO play-2.5+: синхронизировать с версией play.
  val playSlickVsn    = "1.1.1"

  /** Версия bouncy castle. */
  val bcVsn           = "1.52"

  /** Версия typesafe slick. */
  val slickVsn        = "3.1.1"

  /** Версия драйвера common-slick-driver.
   * Он идёт как бинарная зависимость, поэтому зависимые подпроекты. */
  val sioSlickDrvVsn  = "0.3.0-SNAPSHOT"

}
