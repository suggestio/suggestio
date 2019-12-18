package io.suggest.model.n2.media.storage.swfs

import javax.inject.{Inject, Singleton}

import io.suggest.compress.MCompressAlgo
import io.suggest.fio.{IDataSource, WriteRequest}
import io.suggest.model.n2.media.storage.MStorage.STYPE_FN_FORMAT
import io.suggest.model.n2.media.storage._
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.Replication
import io.suggest.swfs.client.proto.assign.{AssignRequest, IAssignResponse}
import io.suggest.swfs.client.proto.delete.{DeleteRequest, IDeleteResponse}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.get.GetRequest
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.swfs.client.proto.put.{IPutResponse, PutRequest}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json._
import japgolly.univeq._
import play.api.mvc.QueryStringBindable

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
  override def assignNew(): Future[(SwfsStorage, IAssignResponse)] = {
    val areq = AssignRequest(DATA_CENTER_DFLT, REPLICATION_DFLT)
    for {
      resp <- client.assign(areq)
    } yield {
      SwfsStorage(resp.fidParsed) -> resp
    }
  }

  override def getStorageHost(ptr: SwfsStorage): Future[Seq[MHostInfo]] = {
    for {
      lookupResp <- volCache.getLocations( ptr.fid.volumeId )
    } yield {
      LOGGER.trace(s"getAssignedStorage($ptr): Resp => ${lookupResp.mkString(", ")}")
      // TODO Берём только первый ответ, с остальными что делать-то?
      _assignedStorResp(ptr, lookupResp)
    }
  }

  override def getStoragesHosts(ptrs: Iterable[T]): Future[Map[T, Seq[MHostInfo]]] = {
    // Собрать множество всех необходимых volumeId.
    val volumeId2ptrs = ptrs
      .groupBy(_.fid.volumeId)

    val perVolFut = Future.traverse(volumeId2ptrs.toSeq) { case (volumeId, volPtrs) =>
      for {
        lookupResp <- volCache.getLocations( volPtrs.head.fid.volumeId )
      } yield {
        LOGGER.trace(s"getAssignedStorages(): Resp for vol#$volumeId => ${lookupResp.mkString(", ")}")
        for (ptr <- volPtrs) yield {
          ptr -> _assignedStorResp(ptr, lookupResp)
        }
      }
    }

    for (perVol <- perVolFut) yield {
      perVol
        .iterator
        .flatten
        .toMap
    }
  }

  /** Общий код сборки результата getAssignedStorage() и getAssignedStorages(). */
  private def _assignedStorResp(ptr: T, lookupResp: Seq[IVolumeLocation]): Seq[MHostInfo] = {
    for (r <- lookupResp) yield {
      MHostInfo(
        nameInt       = r.url,
        namePublic    = r.publicUrl
      )
    }
  }


  /** Короткий код для получения списка локаций volume, связанного с [[SwfsStorages]]. */
  private def _vlocsFut(ptr: T) = volCache.getLocations(ptr.fid.volumeId)

  override def read(ptr: SwfsStorage, acceptCompression: Iterable[MCompressAlgo]): Future[IDataSource] = {
    for {
      vlocs   <- _vlocsFut(ptr)
      getResp <- {
        val getReq = GetRequest(
          volUrl              = vlocs.head.url,
          fid                 = ptr.fid.toString,
          acceptCompression   = acceptCompression
        )
        client.get(getReq)
      }
    } yield {
      getResp.get
    }
  }

  override def delete(ptr: T): Future[Option[IDeleteResponse]] = {
    lazy val logPrefix = s"delete(${ptr.fid}):"
    for {
      vlocs   <- _vlocsFut(ptr)
      delResp <- {
        LOGGER.trace(s"$logPrefix vlocs = [${vlocs.mkString(", ")}]")
        val delReq = DeleteRequest(
          volUrl  = vlocs.head.url,
          fid     = ptr.fid.toString
        )
        client.delete(delReq)
      }
    } yield {
      LOGGER.trace(s"$logPrefix Delete ${ptr.fid} returned: $delResp")
      delResp
    }
  }

  override def write(ptr: T, data: WriteRequest): Future[IPutResponse] = {
    for {
      vlocs     <- _vlocsFut(ptr)
      putResp   <- {
        val putReq = PutRequest.fromRr(
          volUrl        = vlocs.head.url,
          fid           = ptr.fid.toString,
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
        fid    = ptr.fid.toString
      )
      client.isExist(getReq)
    }
  }

  override def FORMAT = SwfsStorage.FORMAT

}


/** Совсем статический объект-компаниьон для инстансов модели.
  * Однако, всё его содержимое можно перекинуть назад в [[SwfsStorages]]. */
object SwfsStorage {

  /** Поддержка биндинга из URL qs. По сути проброс Fid'а, т.к. поле у модели всего одно. */
  implicit def swfsStorageQsb(implicit fidB: QueryStringBindable[Fid]): QueryStringBindable[SwfsStorage] = {
    new QueryStringBindableImpl[SwfsStorage] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SwfsStorage]] = {
        for (fidE <- fidB.bind(key, params)) yield {
          for (fid <- fidE) yield {
            SwfsStorage(
              fid = fid
            )
          }
        }
      }

      override def unbind(key: String, value: SwfsStorage): String = {
        fidB.unbind(key, value.fid)
      }
    }
  }

  /** JSON-маппер для поля file id. */
  private val FID_FORMAT = (__ \ MStorFns.FID.fn).format[Fid]

  /** Поддержка JSON сериализации/десериализации. */
  implicit val FORMAT: OFormat[SwfsStorage] = {
    val READS: Reads[SwfsStorage] = (
      // TODO Opt можно удалить отсюда проверку по STYPE? Она проверяется в IMediaStorage.FORMAT, а тут повторно проверяется.
      STYPE_FN_FORMAT.filter { _ ==* MStorages.SeaWeedFs } and
      FID_FORMAT
    ) { (_, fid) =>
      SwfsStorage(fid)
    }
    val WRITES: OWrites[SwfsStorage] = (
      // TODO Opt можно удалить отсюда проверку по STYPE? Она проверяется в IMediaStorage.FORMAT, а тут повторно проверяется.
      (STYPE_FN_FORMAT: OWrites[MStorage]) and
        FID_FORMAT
      ) { ss =>
      (ss.storageType, ss.fid)
    }
    OFormat(READS, WRITES)
  }

}


/** Инстанс модели [[SwfsStorages]]. Содержит координаты media-блоба внутри SeaweedFS. */
final case class SwfsStorage(fid: Fid)
  extends IMediaStorage
{
  override def storageType = MStorages.SeaWeedFs
  override def storageInfo = fid.toString
}
