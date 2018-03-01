package util.up

import javax.inject.{Inject, Singleton}

import play.api.Configuration

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:49
  * Description: Утиль для аплоада файлов второго поколения.
  * Ориентирована на возможность балансировки файлов между нодами.
  */
@Singleton
class UploadUtil @Inject()(
                            configuration: Configuration
                          ) {

  /**
    * Публичное имя хоста текущего узла.
    * Используется для распределённого хранилища файлов.
    * Ожидается что-то типа "s2.nodes.suggest.io".
    */
  val MY_NODE_PUBLIC_URL = configuration.get[String]("upload.host.my.public")


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
