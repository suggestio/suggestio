package io.suggest.n2.media.storage.swfs

import javax.inject.{Inject, Singleton}
import io.suggest.fio.{IDataSource, MDsReadArgs, WriteRequest}
import io.suggest.n2.media.storage._
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.Replication
import io.suggest.swfs.client.proto.assign.AssignRequest
import io.suggest.swfs.client.proto.delete.{DeleteRequest, DeleteResponse}
import io.suggest.swfs.client.proto.get.GetRequest
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.swfs.client.proto.put.{PutResponse, PutRequest}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import japgolly.univeq._

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
final class SwfsStorage @Inject()(
                                   volCache                      : SwfsVolumeCache,
                                   configuration                 : Configuration,
                                   client                        : ISwfsClient,
                                   implicit private val ec       : ExecutionContext,
                                 )
  extends MacroLogsImpl
  with IMediaStorageStatic
{

  /** Инстанс с дефолтовыми настройками репликации. */
  val REPLICATION_DFLT: Option[Replication] = {
    configuration.getOptional[String]("swfs.assign.replication")
      .map { Replication.apply }
  }

  /** Дефолтовые настройки дата-центра в assign-request. */
  val DATA_CENTER_DFLT: Option[String] = {
    configuration.getOptional[String]("swfs.assign.dc")
  }

  LOGGER.debug(s"Assign settings: dc = $DATA_CENTER_DFLT, replication = $REPLICATION_DFLT")

  /** SWFS имеет полную поддержку HTTP Range. */
  override def canReadRanges = true

  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  override def assignNew(): Future[MAssignedStorage] = {
    val areq = AssignRequest(DATA_CENTER_DFLT, REPLICATION_DFLT)
    for {
      resp <- client.assign(areq)
    } yield {
      MAssignedStorage(
        host    = resp.hostInfo,
        storage = MStorageInfo(
          storage = MStorages.SeaWeedFs,
          data = MStorageInfoData(
            meta = resp.fid,
          ),
        ),
      )
    }
  }

  override def getStorageHost(ptr: MStorageInfoData): Future[Seq[MHostInfo]] = {
    val fid = ptr.swfsFid.get
    for {
      lookupResp <- volCache.getLocations( fid.volumeId )
    } yield {
      LOGGER.trace(s"getAssignedStorage($ptr): Resp => ${lookupResp.mkString(", ")}")
      // TODO Берём только первый ответ, с остальными что делать-то?
      _assignedStorResp(ptr, lookupResp)
    }
  }

  override def getStoragesHosts(ptrs: Iterable[MStorageInfoData]): Future[Map[MStorageInfoData, Seq[MHostInfo]]] = {
    // Собрать множество всех необходимых volumeId.
    if (ptrs.isEmpty) {
      Future.successful( Map.empty )

    } else {
      val volumeId2ptrs = ptrs
        .groupBy(_.swfsFid.get.volumeId)

      for {
        perVol <- Future.traverse( volumeId2ptrs.toSeq ) { case (volumeId, volPtrs) =>
          for {
            lookupResp <- volCache.getLocations( volumeId )
          } yield {
            LOGGER.trace(s"getAssignedStorages(): Resp for vol#$volumeId => ${lookupResp.mkString(", ")}")
            for (ptr <- volPtrs) yield {
              ptr -> _assignedStorResp(ptr, lookupResp)
            }
          }
        }
      } yield {
        perVol
          .iterator
          .flatten
          .toMap
      }
    }
  }

  /** Общий код сборки результата getAssignedStorage() и getAssignedStorages(). */
  private def _assignedStorResp(ptr: MStorageInfoData, lookupResp: Seq[IVolumeLocation]): Seq[MHostInfo] = {
    for (r <- lookupResp) yield {
      MHostInfo(
        nameInt       = r.url,
        namePublic    = r.publicUrl
      )
    }
  }


  /** Короткий код для получения списка локаций volume, связанного с [[SwfsStorage]]. */
  private def _vlocsFut(ptr: MStorageInfoData): Future[Seq[IVolumeLocation]] = {
    val fid = ptr.swfsFid.get
    volCache.getLocations( fid.volumeId )
  }


  /** Асинхронное поточное чтение хранимого файла.
    *
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  override def read(args: MDsReadArgs): Future[IDataSource] = {
    for {
      vlocs   <- _vlocsFut( args.ptr )
      getResp <- {
        val fid = args.ptr.swfsFid.get
        val getReq = GetRequest(
          volUrl              = vlocs.head.url,
          fid                 = fid.toString,
          acceptCompression   = args.acceptCompression,
        )
        client.get(getReq)
      }
    } yield {
      getResp.get
    }
  }

  override def delete(ptr: MStorageInfoData): Future[Option[DeleteResponse]] = {
    val fid = ptr.swfsFid.get
    lazy val logPrefix = s"delete($fid):"
    for {
      vlocs   <- _vlocsFut(ptr)
      delResp <- {
        LOGGER.trace(s"$logPrefix vlocs = [${vlocs.mkString(", ")}]")
        val delReq = DeleteRequest(
          volUrl  = vlocs.head.url,
          fid     = fid.toString
        )
        client.delete(delReq)
      }
    } yield {
      LOGGER.trace(s"$logPrefix Delete $fid returned: $delResp")
      delResp
    }
  }

  override def write(ptr: MStorageInfoData, data: WriteRequest): Future[PutResponse] = {
    for {
      vlocs     <- _vlocsFut(ptr)
      putResp   <- {
        val fid = ptr.swfsFid.get
        val putReq = PutRequest.fromRr(
          volUrl        = vlocs.head.url,
          fid           = fid.toString,
          rr            = data
        )
        client.put(putReq)
      }
    } yield {
      putResp
    }
  }

  override def isExist(ptr: MStorageInfoData): Future[Boolean] = {
    _vlocsFut(ptr).flatMap { vlocs =>
      val fid = ptr.swfsFid.get
      val getReq = GetRequest(
        volUrl = vlocs.head.url,
        fid    = fid.toString
      )
      client.isExist(getReq)
    }
  }

}
