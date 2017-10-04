package util.up

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:49
  * Description: Утиль для аплоада файлов второго поколения.
  * Ориентирована на возможность балансировки файлов между нодами.
  */
class UploadUtil {

  /** Текущее время в часах upload util. */
  def rightNow() = System.currentTimeMillis().milliseconds


  /** Вычислить текущее значение для ttl.
    * @return Секунды.
    */
  def ttlFromNow(now: FiniteDuration = rightNow()): Long = {
    (now + 5.minutes).toSeconds
  }


  /** Является ли значение ttl валидным на текущий момент? */
  def isTtlValid(ttl: Long, now: FiniteDuration = rightNow()): Boolean = {
    ttl.seconds >= now
  }

}
