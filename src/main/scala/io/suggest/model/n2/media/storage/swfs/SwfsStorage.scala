package io.suggest.model.n2.media.storage.swfs

import com.google.inject.{Inject, Singleton}
import io.suggest.fio.IWriteRequest
import io.suggest.model.n2.media.storage.MStorages.STYPE_FN_FORMAT
import io.suggest.model.n2.media.storage._
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.delete.{IDeleteResponse, DeleteRequest}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.get.{IGetResponse, GetRequest}
import io.suggest.swfs.client.proto.put.{IPutResponse, PutRequest}
import io.suggest.util.MacroLogsImpl
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:55
 * Description: Поддержка хранилища SeaWeedFS.
 * @see [[https://github.com/chrislusf/seaweedfs]]
 */
@Singleton
class SwfsStorage_ @Inject() (
  val volCache          : SwfsVolumeCache,
  implicit val client   : ISwfsClient
)
  extends MacroLogsImpl
{

  val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

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
  def assingNew()(implicit ec: ExecutionContext): Future[SwfsStorage] = {
    for( resp <- client.assign() ) yield {
      apply(resp.fidParsed)
    }
  }

}


case class SwfsStorage(fid: Fid, companion: SwfsStorage_)
  extends IMediaStorage
{

  import companion._

  override def sType = MStorages.SeaWeedFs

  override def toJson = Json.toJson(this)

  lazy val _vlocsFut = companion.volCache.getLocations(fid.volumeId)
  
  override def read(implicit ec: ExecutionContext): Future[IGetResponse] = {
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

  override def delete(implicit ex: ExecutionContext): Future[Option[IDeleteResponse]] = {
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

  override def write(data: IWriteRequest)(implicit ec: ExecutionContext): Future[IPutResponse] = {
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

  override def isExist(implicit ec: ExecutionContext): Future[Boolean] = {
    _vlocsFut flatMap { vlocs =>
      val getReq = GetRequest(
        volUrl = vlocs.head.url,
        fid    = fid.toFid
      )
      client.isExist(getReq)
    }
  }

}
