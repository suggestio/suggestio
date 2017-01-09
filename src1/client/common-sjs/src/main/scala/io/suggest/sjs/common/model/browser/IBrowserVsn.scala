package io.suggest.sjs.common.model.browser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 13:53
 * Description: Модель браузерной версии. Бывает необходима.
 */
object MBrowserVsn {

  val VSN_RE = "(\\d+)\\.(\\d+)".r
  
  /** Экстрактор версии браузера: major.minor */
  def parseMajorMinorVsn(vsn: String, offset: Int = 0): Option[MBrowserVsn] = {
    val vsn1 = if (offset > 0)
      vsn.substring(offset)
    else
      vsn
    parseMajorMinorVsn(vsn1)
  }

  def parseMajorMinorVsn(vsn: String): Option[MBrowserVsn] = {
    VSN_RE.findFirstMatchIn(vsn)
      .flatMap { m =>
        try {
          Some( MBrowserVsn(
            vsnMajor = m.group(1).toInt,
            vsnMinor = m.group(2).toInt
          ) )
        } catch {
          case ex: Throwable =>
            println(ex.getClass.getName + " " + ex.getMessage)
            None
        }
      }
  }

  def parseVsnPrefixedFromUa(ua: String, prefix: String): Option[MBrowserVsn] = {
    val i0 = ua.indexOf(prefix)
    if (i0 > 0) {
      parseMajorMinorVsn(ua, i0 + prefix.length)
    } else {
      None
    }
  }
  
}


/** Интерфейс полей версии браузера. */
trait IBrowserVsn {

  /** Номер главной версии. */
  def vsnMajor: Int

  /** Номер младшей версии. Для старых браузеров полезно его знать. */
  def vsnMinor: Int

}


/** Дефолтовая реализация [[IBrowserVsn]]. */
case class MBrowserVsn(
  vsnMajor: Int,
  vsnMinor: Int
) extends IBrowserVsn