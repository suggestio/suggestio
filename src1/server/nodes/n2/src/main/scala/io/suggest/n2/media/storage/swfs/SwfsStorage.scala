package io.suggest.n2.media.storage.swfs

import javax.inject.{Inject, Singleton}
import io.suggest.fio.{IDataSource, MDsReadArgs, WriteRequest}
import io.suggest.n2.media.storage._
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.Replication
import io.suggest.swfs.client.proto.assign.AssignRequest
import io.suggest.swfs.client.proto.delete.{DeleteRequest, DeleteResponse}
import io.suggest.swfs.client.proto.get.{GetRequest, GetResponse}
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.swfs.client.proto.put.{PutRequest, PutResponse}
import io.suggest.swfs.fid.Fid
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import japgolly.univeq._
import play.api.inject.Injector

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
                                   injector                      : Injector,
                                 )
  extends MacroLogsImpl
  with IMediaStorageStatic
{

  private def configuration = injector.instanceOf[Configuration]
  private val client = injector.instanceOf[ISwfsClient]
  private val volCache = injector.instanceOf[SwfsVolumeCache]
  implicit private val ec = injector.instanceOf[ExecutionContext]

  private lazy val (_REPLICATION_DFLT, _DATA_CENTER_DFLT) = {
    /** Инстанс с дефолтовыми настройками репликации. */
    val rep = configuration.getOptional[String]("swfs.assign.replication")
      .map { Replication.apply }
    /** Дефолтовые настройки дата-центра в assign-request. */
    val dcDflt = configuration.getOptional[String]("swfs.assign.dc")
    LOGGER.debug(s"Assign settings: dc = $dcDflt, replication = $rep")
    (rep, dcDflt)
  }

  /** SWFS имеет полную поддержку HTTP Range. */
  override def canReadRanges = true

  /** Получить у swfs-мастера координаты для сохранения нового файла. */
  override def assignNew(): Future[MAssignedStorage] = {
    val areq = AssignRequest(_DATA_CENTER_DFLT, _REPLICATION_DFLT)
    for {
      resp <- client.assign(areq)
    } yield {
      MAssignedStorage(
        host    = resp.hostInfo,
        storage = MStorageInfo(
          storage = MStorages.SeaWeedFs,
          data = MStorageInfoData(
            meta   = resp.fid,
            shards = Set.empty + resp.fidParsed.volumeId.toString,
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
  private def _vlocsFut(fid: Fid): Future[Seq[IVolumeLocation]] =
    volCache.getLocations( fid.volumeId )


  /** Асинхронное поточное чтение хранимого файла.
    *
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  override def read(args: MDsReadArgs): Future[IDataSource] = {
    val fid = args.ptr.swfsFid.get

    lazy val logPrefix = s"read($fid)#${System.currentTimeMillis()}:"

    def __tryVloc( vlocsRest: Seq[IVolumeLocation] ): Future[Option[GetResponse]] = {
      vlocsRest.headOption.fold [Future[Option[GetResponse]]] {
        Future.failed( new NoSuchElementException(s"$logPrefix No more volume locations") )

      } { vlocHead =>
        val getReq = GetRequest(
          volUrl              = vlocHead.url,
          fid                 = fid.toString,
          params              = args.params,
        )
        client.get( getReq )
          .filter( _.nonEmpty )
          .recoverWith { case ex =>
            val errMsg = s"$logPrefix Cannot read from volume ${vlocHead.url} (public ${vlocHead.publicUrl})"
            if (ex.isInstanceOf[NoSuchElementException])
              LOGGER.warn( errMsg )
            else
              LOGGER.warn( errMsg, ex )

            __tryVloc( vlocsRest.tail )
          }
      }
    }

    for {
      vlocs       <- _vlocsFut( fid )
      getRespOpt  <- __tryVloc( vlocs )
    } yield {
      getRespOpt.get
    }
  }


  override def delete(ptr: MStorageInfoData): Future[Option[DeleteResponse]] = {
    val fid = ptr.swfsFid.get
    lazy val logPrefix = s"delete($fid):"
    for {
      vlocs   <- _vlocsFut( fid )
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
    val fid = ptr.swfsFid.get
    for {
      vlocs     <- _vlocsFut( fid )
      putResp   <- {
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
    val fid = ptr.swfsFid.get
    _vlocsFut( fid ).flatMap { vlocs =>
      val getReq = GetRequest(
        volUrl = vlocs.head.url,
        fid    = fid.toString
      )
      client.isExist(getReq)
    }
  }

}
