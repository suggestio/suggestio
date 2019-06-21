package io.suggest.session

import io.suggest.util.logs.MacroLogsDyn
import play.api.mvc.Session

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 16:21
 * Description: Модель допустимых значений ttl в сессии.
 */

object Ttl extends MacroLogsDyn {

  def apply(session: Session): Ttl = {
    apply(session.get(MSessionKeys.RememberMe.value))
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
    sessionValue.fold(acc0) { v =>
      (MSessionKeys.RememberMe.value -> v) :: acc0
    }
  }

  /** Вычесть из состояния TTL указанное кол-во секунд, если требуется/возможно. */
  def minusTtl(seconds: Int): Ttl = this
}


/** Стандартный длинный ttl. */
object LongTtl extends Ttl {

  val SESSION_VALUE = "l"

  override val ttlSeconds: Long = {
    7.days.toSeconds
  }

  override def sessionValue: Option[String] = Some(SESSION_VALUE)
}


/** Стандартный короткий ttl. */
object ShortTtl extends Ttl {

  override val ttlSeconds: Long = {
    20.minutes.toSeconds
  }

  override def sessionValue: Option[String] = None
}


object CustomTtl {

  /** Использовать жесткий ttl? Если да, то ttl логина будет привязан к исходному таймеру. */
  def USE_HARD_TTL = false

}


/** Кастомный ttl, задаваемый извне, например внешним сервисом логина. */
case class CustomTtl(ttlSeconds: Long) extends Ttl {
  import CustomTtl._

  override def sessionValue: Option[String] = Some(ttlSeconds.toString)

  override def minusTtl(seconds: Int): Ttl = {
    if (USE_HARD_TTL)
      copy(ttlSeconds = ttlSeconds - seconds)
    else
      super.minusTtl(seconds)
  }
}

