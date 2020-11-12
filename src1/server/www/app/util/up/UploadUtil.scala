package util.up

import java.nio.file.Path

import io.suggest.crypto.hash.HashesHex
import javax.inject.{Inject, Singleton}
import models.mup.{MDownLoadQs, MUploadFileHandler, MUploadFileHandlers}
import play.api.Configuration
import play.api.inject.Injector
import util.up.ctx.{IAnyFileUploadCtxFactory, IImgUploadCtxFactory, IUploadCtx}

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
final class UploadUtil @Inject()(
                                  injector: Injector,
                                ) {

  private def configuration = injector.instanceOf[Configuration]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private lazy val imgUploadCtxFactory = injector.instanceOf[IImgUploadCtxFactory]
  private lazy val anyFileUploadCtxFactory = injector.instanceOf[IAnyFileUploadCtxFactory]


  /**
    * Публичное имя хоста текущего узла.
    * Используется для распределённого хранилища файлов.
    * Ожидается что-то типа "s2.nodes.suggest.io".
    */
  lazy val MY_NODE_PUBLIC_HOST = configuration.get[String]("upload.host.my.public")

  /** Из-за особенностей play-framework, приходится избыточные сроки жизни ссылок.
    * Проблема на длинном аплоаде (более 1.5 минут) экшен вызывается дважды,
    * и TTL не должен истечь к окончанию аплоада.
    */
  def LINK_TTL = 4.hours

  /** Текущее время в часах upload util. */
  def rightNow(): FiniteDuration = fromMs( System.currentTimeMillis() )
  def fromMs(ms: Long): FiniteDuration = ms.milliseconds


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
      // Без validTillS, т.к. это ломает кэширование и неудобно при публикации ссылок на стороне.
      dispInline  = false,
      hashesHex   = hashesHex,
      // Без clientAddr, т.к. тут ссылка через CDN
    )
  }


  /** Вернуть инстанс IUploadCtx на основе upload-аргументов.
    *
    * @param path Путь до Upload-файла.
    * @return Инстанс IUploadCtx.
    */
  def makeUploadCtx( path: Path, fileHandler: Option[MUploadFileHandler] ): IUploadCtx = {
    fileHandler
      .fold [IUploadCtx] {
        anyFileUploadCtxFactory.make( path )
      } {
        case MUploadFileHandlers.Image =>
          imgUploadCtxFactory.make( path )
      }
  }

}
