package util.img.cron

import java.nio.file.Files
import javax.inject.Inject

import io.suggest.async.AsyncUtil
import io.suggest.img.MImgFmts
import io.suggest.util.logs.MacroLogsImpl
import models.im.{MDynImgId, MImgs3, MLocalImg, MLocalImgs}
import models.mcron.MCronTask
import models.mproj.ICommonDi
import org.apache.commons.io.FileUtils
import util.cron.ICronTasksProvider

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.16 14:19
  * Description: Надо периодичеки удалять директории с картинками, если они долго лежат,
  * а в permanent ещё/уже нет картинок с данным id.
  */
class LocalImgsDeleteNotExistingInPermanent @Inject()(
                                                       mLocalImgs  : MLocalImgs,
                                                       mImgs3      : MImgs3,
                                                       asyncUtil   : AsyncUtil,
                                                       mCommonDi   : ICommonDi
                                                     )
  extends ICronTasksProvider
  with MacroLogsImpl
{

  import mCommonDi._

  private def DNEIP_CONF_PREFIX = "m.img.local.dneip"

  /** Задержка первого запуска после старта play. */
  private def DNEIP_START_DELAY = 15.seconds

  /** Как часто проводить проверки? */
  private def DNEIP_EVERY = 3.hours

  private def DNEIP_OLD_DIR_AGE = 2.hours


  private def _dneipCronTask = MCronTask(
    startDelay  = DNEIP_START_DELAY,
    every       = DNEIP_EVERY,
    displayName = DNEIP_CONF_PREFIX,
  ) { () =>
    for (ex <- dneipFindAndDeleteAsync().failed)
      LOGGER.error("DNEIP: Clean-up failed.", ex)
  }

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): Iterable[MCronTask] = {
    _dneipCronTask #::
    LazyList.empty
  }

  def dneipFindAndDeleteAsync(): Future[_] = {
    Future {
      dneipFindAndDelete()
    }
  }

  def dneipFindAndDelete(): Unit = {
    val dirStream = Files.newDirectoryStream( mLocalImgs.DIR.toPath )
    try {
      val oldNow = System.currentTimeMillis() - DNEIP_OLD_DIR_AGE.toMillis
      dirStream
        .iterator()
        .asScala
        .map(_.toFile)
        // TODO Частые проверки должны отрабатывать только свежие директории. Редкие - все директории.
        .filter { f  =>  f.isDirectory && f.lastModified() < oldNow }
        .foreach { currDir =>
          val rowKeyStr = currDir.getName
          val dynImgId = MDynImgId(rowKeyStr, MImgFmts.default, Nil)
          val mimg = MLocalImg(dynImgId).toWrappedImg
          mImgs3
            .existsInPermanent(mimg)
            .filter(!_)
            .andThen { case _: Success[_] =>
              LOGGER.debug("Deleting permanent-less img-dir: " + currDir)
              FileUtils.deleteDirectory(currDir)
            }(asyncUtil.singleThreadIoContext)
            .failed
            .foreach {
              case _: NoSuchElementException =>
                // do nothing
              case ex =>
                LOGGER.error("DNEIP: Failed to process directory " + currDir, ex)
            }
        }
    } finally {
      dirStream.close()
    }
  }

}
