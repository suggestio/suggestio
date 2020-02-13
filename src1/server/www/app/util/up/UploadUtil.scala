package util.up

import io.suggest.crypto.hash.HashesHex
import javax.inject.{Inject, Singleton}
import models.mup.MDownLoadQs
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.ExecutionContext
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
                            injector: Injector,
                          ) {

  private def configuration = injector.instanceOf[Configuration]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /**
    * Публичное имя хоста текущего узла.
    * Используется для распределённого хранилища файлов.
    * Ожидается что-то типа "s2.nodes.suggest.io".
    */
  lazy val MY_NODE_PUBLIC_URL = configuration.get[String]("upload.host.my.public")

  def LINK_TTL = 30.seconds

  /** Текущее время в часах upload util. */
  def rightNow() = System.currentTimeMillis().milliseconds


  /** Вычислить текущее значение для ttl.
    * @return Секунды.
    */
  def ttlFromNow( now: FiniteDuration = rightNow(),
                  linkTtl: FiniteDuration = LINK_TTL ): Long = {
    (now + LINK_TTL).toSeconds
  }


  /** Является ли значение ttl валидным на текущий момент? */
  def isTtlValid(ttl: Long, now: FiniteDuration = rightNow()): Boolean = {
    ttl.seconds >= now
  }


  /** Сборка аргументов для будущей ссылки скачивания файла.
    *
    * @param fileNodeId id файлового узла.
    * @param hashesHex Результат dlQsHashesHex().
    * @return qs для сборки ссылки через routes.Upload.download().
    */
  def mkDlQs(fileNodeId: String, hashesHex: HashesHex): MDownLoadQs = {
    MDownLoadQs(
      nodeId      = fileNodeId,
      // Чтобы не тратить ресурсы на разбор давно неактуальных древних ссылок, ограничиваем ссылки по времени жизни:
      validTillS  = ttlFromNow( linkTtl = 3.days ),
      dispInline  = false,
      hashesHex   = hashesHex,
      // Без clientAddr, т.к. тут ссылка через CDN
    )
  }

}
