package util.cdn

import javax.inject.{Inject, Singleton}

import io.suggest.file.up.MFile4UpProps
import io.suggest.model.n2.media.storage._
import io.suggest.model.n2.media.storage.swfs.{SwfsStorage, SwfsStorages, SwfsVolumeCache}
import io.suggest.util.logs.MacroLogsImpl
import models.mup.MSwfsFidInfo
import japgolly.univeq._
import util.up.UploadUtil
import io.suggest.model.n2.media.MMedia
import io.suggest.swfs.client.proto.lookup.IVolumeLocation
import io.suggest.url.MHostInfo

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.10.17 16:08
  * Description: Утиль для распределения медиа (файлов) по кластеру s.io.
  *
  * По сути, некая абстракция между нижележащей SeaWeedFS и всякими контроллерами.
  */
@Singleton
class DistUtil @Inject()(
                          val iMediaStorages        : IMediaStorages,
                          uploadUtil                : UploadUtil,
                          swfsVolumeCache           : SwfsVolumeCache,
                          implicit private val ec   : ExecutionContext
                        )
  extends MacroLogsImpl
{

  /** Используемое хранилище. */
  private def DIST_STORAGE: MStorage = MStorages.SeaWeedFs


  // TODO DIST_IMG Реализовать поддержку распределения media-файлов по нодам.

  /** Подготовить местечко для сохранения нового файла, вернув данные сервера для заливки файла.
    *
    * @param upProps Обобщённые данные по заливаемому файлу.
    * @return Фьючерс с результатом.
    */
  def assignDist(upProps: MFile4UpProps): Future[MAssignedStorage] = {
    val storageType = DIST_STORAGE
    val storageFacade = iMediaStorages.getModel( storageType ).asInstanceOf[SwfsStorages]
    val assignRespFut = storageFacade.assignNew()

    for {
      assignResp <- assignRespFut
    } yield {
      LOGGER.trace(s"assignDist[${System.currentTimeMillis()}]: Assigned ok:\n props = $upProps\n resp = $assignResp")
      val swfsAssignResp = assignResp._2
      MAssignedStorage(
        host    = swfsAssignResp.hostInfo,
        storage = assignResp._1
      )
    }
  }


  /** Проверка возможности аплоада прямо сюда на текущую ноду.
    *
    * @param storage Строка инфы в контексте данного хранилища.
    * @return Расширенные данные для аплоада, если Some.
    *         None, значит доступ закрыт.
    */
  def checkStorageForThisNode(storage: IMediaStorage): Future[Either[Seq[IVolumeLocation], MSwfsFidInfo]] = {
    // Распарсить Swfs FID из URL и сопоставить полученный volumeID с текущей нодой sio.
    lazy val logPrefix = s"checkForUpload($storage)#${System.currentTimeMillis()}:"

    storage match {
      case swfsStorage: SwfsStorage =>
        for {
          volLocs <- swfsVolumeCache.getLocations( swfsStorage.fid.volumeId )
        } yield {
          // Может быть несколько результатов, если у volume существуют реплики.
          // Нужно найти целевую мастер-шарду, которая располагается где-то очень близко к текущему локалхосту.
          val myExtHost = uploadUtil.MY_NODE_PUBLIC_URL
          volLocs
            .find { volLoc =>
              volLoc.publicUrl ==* myExtHost
              // Не проверяем nameInt/url, потому что там полу-рандомный порт swfs
            }
            .map { myVol =>
              LOGGER.trace(s"$logPrefix Ok, local vol = $myVol\n fid = ${swfsStorage.fid.toString}\n all vol locs = ${volLocs.mkString(", ")}")
              MSwfsFidInfo(swfsStorage.fid, myVol, volLocs)
            }
            .toRight {
              LOGGER.error(s"$logPrefix Failed to find vol#${swfsStorage.fid.volumeId} for fid='${swfsStorage.fid}' nearby. My=$myExtHost, storage=$storage Other available volumes considered non-local: ${volLocs.mkString(", ")}")
              volLocs
            }
        }
    }
  }


  /** Вернуть карту медиа-хосты для указанных media.
    *
    * @param medias Список интересующих media.
    * @return Карта nodeId -> hostname.
    */
  def mediasHosts(medias: Iterable[MMedia]): Future[Map[String, Seq[MHostInfo]]] = {
    for {
      storages2hostsMap <- iMediaStorages.getStoragesHosts( medias.map(_.storage) )
    } yield {
      medias
        .iterator
        .map { media =>
          media.nodeId -> storages2hostsMap(media.storage)
        }
        .toMap
    }
  }


}
