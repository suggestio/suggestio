package models.msession

import play.api.mvc.Session
import util.PlayMacroLogsDyn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 15:43
 * Description: session timestamp - модель для доступа к таймштампу, сохраненному в сессии.
 */

object LoginTimestamp extends PlayMacroLogsDyn {

  /** Чтобы не делать лишних движений, фиксируем начало времён, вычитая его из всех timestamp'ов. */
  def TSAMP_SUBSTRACT = 1402927907242L

  /** Сгенерить текущий timestamp. */
  def currentTstamp = (System.currentTimeMillis() - TSAMP_SUBSTRACT) / 1000L


  def parseTstamp(tstampStr: String): Option[Long] = {
    try {
      Some(tstampStr.toLong)
    } catch {
      case ex: Exception =>
        LOGGER.warn(s"invokeBlock(): Failed to parse session timestamp: raw = $tstampStr")
        None
    }
  }


  def fromSession(session: Session): Option[LoginTimestamp] = {
    val stkOpt = session get Keys.Timestamp.name
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


import LoginTimestamp._

case class LoginTimestamp(tstamp: Long, ttl: Ttl) {

  def isTimestampValid(now: Long = currentTstamp): Boolean = {
    ttl.isTimestmapValid(tstamp, now)
  }

  def toSessionKeys: List[(String, String)] = {
    val acc = List(Keys.Timestamp.name -> tstamp.toString)
    ttl.addToSessionAcc(acc)
  }

  def addToSession(session0: Session): Session = {
    session0.copy(
      data = session0.data ++ toSessionKeys
    )
  }

}

