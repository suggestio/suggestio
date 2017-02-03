package util.img.cron

import java.nio.file.Files

import com.google.inject.Inject
import io.suggest.async.AsyncUtil
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.{MImgs3, MLocalImg, MLocalImgs}
import models.mcron.{ICronTask, MCronTask}
import models.mproj.ICommonDi
import org.apache.commons.io.FileUtils
import util.cron.ICronTasksProvider

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.16 14:19
  * Description: Надо периодичеки удалять директории с картинками, если они долго лежат,
  * а в permanent ещё/уже нет картинок с данным id.
  */
class PeriodicallyDeleteNotExistingInPermanent @Inject() (
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

  /** Включено ли автоудаление директорий? */
  def DNEIP_ENABLED = configuration.getBoolean(DNEIP_CONF_PREFIX + ".enabled") getOrElse true

  /** Задержка первого запуска после старта play. */
  def DNEIP_START_DELAY = configuration.getInt(DNEIP_CONF_PREFIX + ".start.delay.seconds")
    .getOrElse(15)
    .seconds

  /** Как часто проводить проверки? */
  def DNEIP_EVERY = configuration.getInt(DNEIP_CONF_PREFIX + ".every.minutes")
    .fold[FiniteDuration] (3.hours)(_.minutes)

  def DNEIP_OLD_DIR_AGE_MS: Long = configuration.getInt(DNEIP_CONF_PREFIX + ".old.age.minutes")
    .fold(2.hours)(_.minutes)
    .toMillis


  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): TraversableOnce[ICronTask] = {
    if (DNEIP_ENABLED) {
      val task = MCronTask(startDelay = DNEIP_START_DELAY, every = DNEIP_EVERY, displayName = DNEIP_CONF_PREFIX) {
        dneipFindAndDeleteAsync().onFailure { case ex =>
          LOGGER.error("DNEIP: Clean-up failed.", ex)
        }
      }
      List(task)
    } else {
      Nil
    }
  }

  def dneipFindAndDeleteAsync(): Future[_] = {
    Future {
      dneipFindAndDelete()
    }
  }

  def dneipFindAndDelete(): Unit = {
    val dirStream = Files.newDirectoryStream( mLocalImgs.DIR.toPath )
    try {
      val oldNow = System.currentTimeMillis() - DNEIP_OLD_DIR_AGE_MS
      dirStream.iterator()
        .map(_.toFile)
        // TODO Частые проверки должны отрабатывать только свежие директории. Редкие - все директории.
        .filter { f  =>  f.isDirectory && f.lastModified() < oldNow }
        .foreach { currDir =>
          val rowKeyStr = currDir.getName
          val rowKey = UuidUtil.base64ToUuid(rowKeyStr)
          val mimg = MLocalImg(rowKey).toWrappedImg
          mImgs3.existsInPermanent(mimg)
            .filter(!_)
            .andThen { case _: Success[_] =>
              LOGGER.debug("Deleting permanent-less img-dir: " + currDir)
              FileUtils.deleteDirectory(currDir)
            }(asyncUtil.singleThreadIoContext)
            .onFailure {
              case ex: NoSuchElementException =>
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
