package models.crawl

import java.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.11.14 16:29
 * Description: Элемент URL, представляющий данные по одной, подлежащей кравлингу.
 */
case class SiteMapUrl(
  loc         : String,
  lastMod     : Option[LocalDate] = None,
  changeFreq  : Option[ChangeFreq] = None,
  priority    : Option[Float] = None
)


import enumeratum.{Enum, EnumEntry}

/** Изменяемость страниц. */
object ChangeFreqs extends Enum[ChangeFreq] {
  case object hourly extends ChangeFreq
  case object daily extends ChangeFreq
  case object weekly extends ChangeFreq
  //... always, monthly, yearly, never

  override def values = findValues
}
sealed abstract class ChangeFreq extends EnumEntry
