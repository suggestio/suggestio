package io.suggest.model.n2.media.storage.swfs

import javax.inject.{Inject, Singleton}
import io.suggest.fio.IWriteRequest
import io.suggest.model.n2.media.storage.MStorage.STYPE_FN_FORMAT
import io.suggest.model.n2.media.storage._
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.Replication
import io.suggest.swfs.client.proto.assign.AssignRequest
import io.suggest.swfs.client.proto.delete.{DeleteRequest, IDeleteResponse}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.get.{GetRequest, IGetResponse}
import io.suggest.swfs.client.proto.put.{IPutResponse, PutRequest}
import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:55
 * Description: Поддержка хранилища SeaWeedFS.
 *
 * @see [[https://github.com/chrislusf/seaweedfs]]
 */
@Singleton
class SwfsStorages @Inject() (
  volCache                      : SwfsVolumeCache,
  configuration                 : Configuration,
  client                        : ISwfsClient,
  implicit private val ec       : ExecutionContext
)
  extends MacroLogsImpl
  with IMediaStorageStaticImpl
{

  override type T = SwfsStorage

  /** Инстанс с дефолтовыми настройками репликации. */
  val REPLICATION_DFLT: Option[Replication] = {
    configuration.getOptional[String]("swfs.assign.replication")
      .map { Replication.apply }
  }

  /** Дефолтовые настройки дата-центра в assign-request. */
  val DATA_CENTER_DFLT: Option[String] = {
    configuration.getOptional[String]("swfs.assign.dc")
  }

  LOGGER.info(s"Assign settings: dc = $DATA_CENTER_DFLT, replication = $REPLICATION_DFLT")


  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  override def assignNew(): Future[SwfsStorage] = {
    val areq = AssignRequest(DATA_CENTER_DFLT, REPLICATION_DFLT)
    for {
      resp <- client.assign(areq)
    } yield {
      SwfsStorage(resp.fidParsed)
    }
  }


  /** Короткий код для получения списка локаций volume, связанного с [[SwfsStorages]]. */
  private def _vlocsFut(ptr: T) = volCache.getLocations(ptr.fid.volumeId)

  override def read(ptr: T): Future[IGetResponse] = {
    for {
      vlocs   <- _vlocsFut(ptr)
      getResp <- {
        val getReq = GetRequest(
          volUrl = vlocs.head.url,
          fid    = ptr.fid.toFid
        )
        client.get(getReq)
      }
    } yield {
      getResp.get
    }
  }

  override def delete(ptr: T): Future[Option[IDeleteResponse]] = {
    for {
      vlocs   <- _vlocsFut(ptr)
      delResp <- {
        val delReq = DeleteRequest(
          volUrl  = vlocs.head.url,
          fid     = ptr.fid.toFid
        )
        client.delete(delReq)
      }
    } yield {
      LOGGER.trace(s"Delete ${ptr.fid} returned: $delResp")
      delResp
    }
  }

  override def write(ptr: T, data: IWriteRequest): Future[IPutResponse] = {
    for {
      vlocs     <- _vlocsFut(ptr)
      putResp   <- {
        val putReq = PutRequest.fromRr(
          volUrl        = vlocs.head.url,
          fid           = ptr.fid.toFid,
          rr            = data
        )
        client.put(putReq)
      }
    } yield {
      putResp
    }
  }

  override def isExist(ptr: T): Future[Boolean] = {
    _vlocsFut(ptr).flatMap { vlocs =>
      val getReq = GetRequest(
        volUrl = vlocs.head.url,
        fid    = ptr.fid.toFid
      )
      client.isExist(getReq)
    }
  }

  override def FORMAT = SwfsStorage.FORMAT

}


/** Совсем статический объект-компаниьон для инстансов модели.
  * Однако, всё его содержимое можно перекинуть назад в [[SwfsStorages]]. */
object SwfsStorage {

  /** JSON-маппер для поля file id. */
  private val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

  /** Поддержка JSON сериализации/десериализации. */
  implicit val FORMAT: OFormat[SwfsStorage] = {
    val READS: Reads[SwfsStorage] = (
      // TODO Opt можно удалить отсюда проверку по STYPE? Она проверяется в IMediaStorage.FORMAT, а тут повторно проверяется.
      STYPE_FN_FORMAT.filter { _ == MStorages.SeaWeedFs } and
        FID_FORMAT
      ) { (_, fid) =>
      SwfsStorage(fid)
    }
    val WRITES: OWrites[SwfsStorage] = (
      // TODO Opt можно удалить отсюда проверку по STYPE? Она проверяется в IMediaStorage.FORMAT, а тут повторно проверяется.
      (STYPE_FN_FORMAT: OWrites[MStorage]) and
        FID_FORMAT
      ) { ss =>
      (ss.sType, ss.fid)
    }
    OFormat(READS, WRITES)
  }

}


/** Инстанс модели [[SwfsStorages]]. Содержит координаты media-блоба внутри SeaweedFS. */
case class SwfsStorage(fid: Fid)
  extends IMediaStorage
{
  override def sType = MStorages.SeaWeedFs
}
