package io.suggest.model.n2.media.storage.swfs

import com.google.inject.{Inject, Singleton}
import io.suggest.fio.IWriteRequest
import io.suggest.model.n2.media.storage.MStorages.STYPE_FN_FORMAT
import io.suggest.model.n2.media.storage._
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.Replication
import io.suggest.swfs.client.proto.assign.AssignRequest
import io.suggest.swfs.client.proto.delete.{IDeleteResponse, DeleteRequest}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.get.{IGetResponse, GetRequest}
import io.suggest.swfs.client.proto.put.{IPutResponse, PutRequest}
import io.suggest.util.MacroLogsImpl
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

  /** JSON-маппер для поля file id. */
  val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

  /** Инстанс с дефолтовыми настройками репликации. */
  val REPLICATION_DFLT: Option[Replication] = {
    configuration.getString("swfs.assign.replication")
      .map { Replication.apply }
  }

  /** Дефолтовые настройки дата-центра в assign-request. */
  val DATA_CENTER_DFLT: Option[String] = {
    configuration.getString("swfs.assign.dc")
  }

  LOGGER.info(s"Assign settings: dc = $DATA_CENTER_DFLT, replication = $REPLICATION_DFLT")


  /** Поддержка JSON сериализации/десериализации. */
  override implicit val FORMAT: OFormat[SwfsStorage] = {
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

}


/** Инстанс модели [[SwfsStorages]]. Содержит координаты media-блоба внутри SeaweedFS. */
case class SwfsStorage(fid: Fid)
  extends IMediaStorage
{
  override def sType = MStorages.SeaWeedFs
}
