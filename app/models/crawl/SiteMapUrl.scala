package models.crawl

import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.11.14 16:29
 * Description: Элемент URL, представляющий данные по одной, подлежащей кравлингу.
 */
trait SiteMapUrlT {
  def loc         : String
  def lastMod     : Option[LocalDate]
  def changeFreq  : Option[ChangeFreq]
  def priority    : Option[Float]
}

case class SiteMapUrl(
  loc         : String,
  lastMod     : Option[LocalDate] = None,
  changeFreq  : Option[ChangeFreq] = None,
  priority    : Option[Float] = None
) extends SiteMapUrlT


/** Изменяемость страниц. */
object ChangeFreqs extends Enumeration {
  type ChangeFreq = Value
  val hourly, daily, weekly = Value
  lazy val always, monthly, yearly, never = Value
}
