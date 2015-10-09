package io.suggest.model.n2.media.storage

import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import IMediaStorage.STYPE_FN_FORMAT

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:55
 * Description: Поддержка хранилища SeaWeedFS.
 * @see [[https://github.com/chrislusf/seaweedfs]]
 */
object SwfsStorage {

  val VOLUME_ID_FMT = (__ \ MStorFns.VOLUME_ID.fn).format[Long]

  val FILE_ID_FMT   = (__ \ MStorFns.FILE_ID.fn).format[String]

  implicit val READS: Reads[SwfsStorage] = (
    STYPE_FN_FORMAT.filter { _ == MStorages.SeaWeedFs } and
    VOLUME_ID_FMT and
    FILE_ID_FMT
  ) { (_, volumeId, fileId) =>
    SwfsStorage(volumeId, fileId)
  }

  implicit val WRITES: OWrites[SwfsStorage] = (
    (STYPE_FN_FORMAT: OWrites[MStorage]) and
    VOLUME_ID_FMT and
    FILE_ID_FMT
  ) { ss =>
    (ss.sType, ss.volumeId, ss.fileId)
  }

  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  def assingNew(): Future[SwfsStorage] = {
    ???
  }

}


case class SwfsStorage(
  volumeId  : Long,
  fileId    : String
)
  extends IMediaStorage
{

  override def sType = MStorages.SeaWeedFs

  override def read(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = ???

  override def delete(implicit ex: ExecutionContext): Future[_] = ???

  override def write(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[_] = ???

  override def isExist(implicit ec: ExecutionContext): Future[Boolean] = ???

}
