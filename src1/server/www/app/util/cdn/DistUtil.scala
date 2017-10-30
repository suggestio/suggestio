package util.cdn

import javax.inject.{Inject, Singleton}

import io.suggest.common.empty.OptionUtil
import io.suggest.file.up.MFile4UpProps
import io.suggest.model.n2.media.storage.{IMediaStorages, MStorages}
import io.suggest.model.n2.media.storage.swfs.{SwfsStorages, SwfsVolumeCache}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.util.logs.MacroLogsImpl
import models.mcdn.MAssignedStorage
import models.mup.MSwfsUploadReqInfo
import japgolly.univeq._
import util.up.UploadUtil
import io.suggest.common.fut.FutureUtil.HellImplicits._

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
  private def DIST_STORAGE = MStorages.SeaWeedFs


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

    lazy val logPrefix = s"assignDist[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Started for $upProps")

    for {
      assignResp <- assignRespFut
    } yield {
      LOGGER.trace(s"$logPrefix Assigned swfs resp: $assignResp")
      val swfsAssignResp = assignResp._2
      MAssignedStorage(
        hostExt       = swfsAssignResp.publicUrl,
        hostInt       = swfsAssignResp.url,
        storageType   = storageType,
        storageInfo   = swfsAssignResp.fid
      )
    }
  }


  /** Проверка возможности аплоада прямо сюда на текущую ноду.
    *
    * @param storage Результат assignDist().
    * @return Расширенные данные для аплоада, если Some.
    *         None, значит доступ закрыт.
    */
  def checkForUpload(storage: MAssignedStorage): Future[Option[MSwfsUploadReqInfo]] = {
    // Распарсить Swfs FID из URL и сопоставить полученный volumeID с текущей нодой sio.
    val fidOpt = OptionUtil.maybe( storage.storageType ==* DIST_STORAGE ) {
      Fid( storage.storageInfo )
    }

    lazy val logPrefix = s"checkForUpload[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Checking fid=${fidOpt.orNull}")

    fidOpt.fold [Future[Option[MSwfsUploadReqInfo]]] (None) { fid =>
      for {
        volLocs <- swfsVolumeCache.getLocations( fid.volumeId )
      } yield {
        // Может быть несколько результатов, если у volume существуют реплики.
        // Нужно найти целевую мастер-шарду, которая располагается где-то очень близко к текущему локалхосту.
        val myExtHost = uploadUtil.MY_NODE_PUBLIC_URL
        val myVolOpt = volLocs
          .find { volLoc =>
            (volLoc.publicUrl ==* myExtHost) &&
              (storage.hostInt ==* volLoc.url)
          }

        if (myVolOpt.isEmpty)
          LOGGER.error(s"$logPrefix Failed to find vol#${fid.volumeId} for fid='$fid' nearby. My=$myExtHost, storage=$storage. Other available volumes considered non-local: ${volLocs.mkString(", ")}")

        // Пусть будет NSEE при нарушении, так и надо: .recover() отработает ошибку доступа.
        val myVol = myVolOpt.get
        Some( MSwfsUploadReqInfo(fid, myVol, volLocs) )
      }
    }
  }

}
