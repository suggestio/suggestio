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
  val volCache          : SwfsVolumeCache,
  configuration         : Configuration,
  implicit val client   : ISwfsClient,
  implicit val ec       : ExecutionContext
)
  extends MacroLogsImpl
{

  /** JSON-маппер для поля file id. */
  val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

  /** Инстанс с дефолтовыми настройками репликации. */
  val REPLICATION_DFLT: Option[Replication] = {
    configuration.getString("swfs.assign.replication")
      .map { Replication.apply }
  }

  /** Дефолтовые настройки дата-центра в assign-request. */
  val DATA_CENTER_DFLT: Option[String] = configuration.getString("swfs.assign.dc")

  LOGGER.info(s"Assign settings: dc = $DATA_CENTER_DFLT, replication = $REPLICATION_DFLT")

  // Поддержка JSON сериализации/десериализации.
  val READS: Reads[SwfsStorage] = (
    STYPE_FN_FORMAT.filter { _ == MStorages.SeaWeedFs } and
    FID_FORMAT
  ) { (_, fid) =>
    apply(fid)
  }

  val WRITES: OWrites[SwfsStorage] = (
    (STYPE_FN_FORMAT: OWrites[MStorage]) and
    FID_FORMAT
  ) { ss =>
    (ss.sType, ss.fid)
  }

  implicit val FORMAT = Format(READS, WRITES)

  def apply(fid: Fid): SwfsStorage = {
    SwfsStorage(fid, this)
  }

  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  def assingNew(): Future[SwfsStorage] = {
    val areq = AssignRequest(DATA_CENTER_DFLT, REPLICATION_DFLT)
    for {
      resp <- client.assign(areq)
    } yield {
      apply(resp.fidParsed)
    }
  }

}


case class SwfsStorage(fid: Fid, companion: SwfsStorages)
  extends IMediaStorage
{

  import companion._

  override def sType = MStorages.SeaWeedFs

  override def toJson = Json.toJson(this)

  lazy val _vlocsFut = companion.volCache.getLocations(fid.volumeId)
  
  override def read(): Future[IGetResponse] = {
    for {
      vlocs   <- _vlocsFut
      getResp <- {
        val getReq = GetRequest(
          volUrl = vlocs.head.url,
          fid    = fid.toFid
        )
        client.get(getReq)
      }
    } yield {
      getResp.get
    }
  }

  override def delete(): Future[Option[IDeleteResponse]] = {
    for {
      vlocs   <- _vlocsFut
      delResp <- {
        val delReq = DeleteRequest(
          volUrl  = vlocs.head.url,
          fid     = fid.toFid
        )
        client.delete(delReq)
      }
    } yield {
      LOGGER.trace(s"Delete $fid returned $delResp")
      delResp
    }
  }

  override def write(data: IWriteRequest): Future[IPutResponse] = {
    for {
      vlocs     <- _vlocsFut
      putResp   <- {
        val putReq = PutRequest.fromRr(
          volUrl        = vlocs.head.url,
          fid           = fid.toFid,
          rr            = data
        )
        client.put(putReq)
      }
    } yield {
      putResp
    }
  }

  override def isExist: Future[Boolean] = {
    _vlocsFut flatMap { vlocs =>
      val getReq = GetRequest(
        volUrl = vlocs.head.url,
        fid    = fid.toFid
      )
      client.isExist(getReq)
    }
  }

}
