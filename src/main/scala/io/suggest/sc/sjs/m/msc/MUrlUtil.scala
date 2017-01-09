package io.suggest.sc.sjs.m.msc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.05.16 14:58
  * Description: Модель для работы с URL Hash.
  */
object MUrlUtil {

  /** Префикс URL Hash. */
  def URL_HASH_PREFIX = "#!?"

  def getUrlHash(url: String): Option[String] = {
    val inx = url.indexOf(URL_HASH_PREFIX)
    if (inx < 0) {
      None
    } else {
      Some( url.substring(inx) )
    }
  }

  /** Выбрасывание из URL-хвоста лишнего префикса и прочего. */
  def clearUrlHash(urlHash: String): Option[String] = {
    val nonEmptyF = { s: String =>
      s.nonEmpty
    }
    Option(urlHash)
      .filter(nonEmptyF)
      .map(_.replaceFirst("^[#!?]+", ""))
      .filter(nonEmptyF)
  }

}
