package models.msession

import play.api.Play.{current, configuration}
import play.api.mvc.Session
import util.PlayMacroLogsDyn
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 16:21
 * Description: Модель допустимых значений ttl в сессии.
 */

object Ttl extends PlayMacroLogsDyn {

  def apply(session: Session): Ttl = {
    apply(session.get(Keys.RememberMe.name))
  }
  def apply(s: Option[String]): Ttl = {
    if (s.isDefined) apply(s.get) else ShortTtl
  }
  def apply(s: String): Ttl = {
    s match {
      case LongTtl.SESSION_VALUE =>
        LongTtl
      case _ if s.matches("\\d{1,10}") =>
        // Ограничиваем допустимое время жизни
        val ttlSeconds = Math.min(LongTtl.ttlSeconds, s.toLong)
        CustomTtl(ttlSeconds)
      case _ =>
        LOGGER.warn("Unknown session ttl: " + s)
        ShortTtl
    }
  }

}

/** Абстрактный режим ttl. */
sealed trait Ttl {
  /** Время жизни в секундах. */
  def ttlSeconds: Long

  /** Проверить валидность таймштампа. */
  def isTimestmapValid(tstamp: Long, now: Long): Boolean = {
    tstamp + ttlSeconds >= now
  }

  def sessionValue: Option[String]
  def addToSessionAcc(acc0: List[(String, String)]): List[(String, String)] = {
    sessionValue match {
      case Some(v) => Keys.RememberMe.name -> v :: acc0
      case None    => acc0
    }
  }
}


/** Стандартный длинный ttl. */
object LongTtl extends Ttl {
  val SESSION_VALUE = "l"

  override val ttlSeconds: Long = {
    configuration.getInt("login.session.long.ttl.days")
      .getOrElse(14)   // первоначальный дефолт - две недели
      .days
      .toSeconds
  }

  override def sessionValue: Option[String] = Some(SESSION_VALUE)
}


/** Стандартный короткий ttl. */
object ShortTtl extends Ttl {
  override val ttlSeconds: Long = {
    configuration.getInt("login.session.short.ttl.minutes")
      .getOrElse(20)
      .minutes
      .toSeconds
  }

  override def sessionValue: Option[String] = None
}


/** Кастомный ttl, задаваемый извне, например внешним сервисом логина. */
case class CustomTtl(ttlSeconds: Long) extends Ttl {
  override def sessionValue: Option[String] = Some(ttlSeconds.toString)
}

