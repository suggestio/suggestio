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
  // TODO Большинство элементов можно закинуть в lazy val или удалить, т.к. они не нужны.
  val always, hourly, daily, weekly, monthly, yearly, never = Value
}
