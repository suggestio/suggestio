package io.suggest.session

import io.suggest.util.logs.MacroLogsDyn
import play.api.mvc.Session

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 15:43
 * Description: session timestamp - модель для доступа к таймштампу, сохраненному в сессии.
 */

object LoginTimestamp extends MacroLogsDyn {

  /** Чтобы не делать лишних движений, фиксируем начало времён, вычитая его из всех timestamp'ов. */
  def TSAMP_SUBSTRACT = 1402927907242L

  /** Сгенерить текущий timestamp. */
  def currentTstamp(): Long =
    (System.currentTimeMillis() - TSAMP_SUBSTRACT) / 1000L


  def parseTstamp(tstampStr: String): Option[Long] = {
    try {
      Some(tstampStr.toLong)
    } catch {
      case ex: Exception =>
        LOGGER.warn(s"invokeBlock(): Failed to parse session timestamp: raw = $tstampStr", ex)
        None
    }
  }


  def fromSession(session: Session): Option[LoginTimestamp] = {
    val stkOpt = session.get( MSessionKeys.Timestamp.value )
    fromSession1(stkOpt, session)
  }

  def fromSession1(stkOpt: Option[String], session: Session): Option[LoginTimestamp] = {
    for {
      stkStr <- stkOpt
      stk    <- parseTstamp(stkStr)
    } yield {
      val ttl = Ttl(session)
      LoginTimestamp(stk, ttl)
    }
  }

}


import io.suggest.session.LoginTimestamp._

case class LoginTimestamp(tstamp: Long, ttl: Ttl) {

  def isTimestampValid(now: Long = currentTstamp()): Boolean = {
    ttl.isTimestmapValid(tstamp, now)
  }

  def toSessionKeys: List[(String, String)] = {
    val acc = (MSessionKeys.Timestamp.value -> tstamp.toString) :: Nil
    ttl.addToSessionAcc(acc)
  }

  /**
   * Обновить timestamp, уведомив ttl.
   * @param tstamp1 Новый timestamp.
   * @return Новый экземпляр [[LoginTimestamp]].
   */
  def withTstamp(tstamp1: Long): LoginTimestamp = {
    val diff = tstamp1 - this.tstamp
    val ttl1 = if (diff > 0L) {
      ttl minusTtl diff.toInt
    } else {
      if (diff < 0L)
        LOGGER.warn(s"Negative timestamp update! diff=$diff ($tstamp -> $tstamp1). Check sio servers datetime!")
      ttl
    }
    copy(tstamp = tstamp1, ttl = ttl1)
  }

}

