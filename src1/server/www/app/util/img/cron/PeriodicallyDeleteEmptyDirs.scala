package util.img.cron

import java.nio.file.{Files, Path}

import javax.inject.Inject
import io.suggest.async.AsyncUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.MLocalImgs
import models.mcron.{ICronTask, MCronTask}
import models.mproj.ICommonDi
import util.cron.ICronTasksProvider

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.16 14:09
  * Description: Периодически стирать пустые директории через Cron.
  * Внутри, для работы с ФС, используется java.nio.
  */
class PeriodicallyDeleteEmptyDirs @Inject() (
  mLocalImgs    : MLocalImgs,
  asyncUtil     : AsyncUtil,
  mCommonDi     : ICommonDi
)
  extends ICronTasksProvider
  with MacroLogsImpl
{

  import mCommonDi._


  private def EDD_CONF_PREFIX = "m.img.local.edd"

  /** Включено ли периодическое удаление пустых директорий из под картинок? */
  private def DELETE_EMPTY_DIRS_ENABLED = configuration.getOptional[Boolean](EDD_CONF_PREFIX + ".enabled")
    .contains(true)

  /** Как часто инициировать проверку? */
  private def DELETE_EMPTY_DIRS_EVERY = configuration.getOptional[Int](EDD_CONF_PREFIX + ".every.minutes")
    .fold [FiniteDuration] (12.hours) (_.minutes)

  /** На сколько отодвигать старт проверки. */
  private def DELETE_EMPTY_DIRS_START_DELAY = configuration.getOptional[Int](EDD_CONF_PREFIX + ".start.delay.seconds")
    .getOrElse(60)
    .seconds

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): TraversableOnce[ICronTask] = {
    if (DELETE_EMPTY_DIRS_ENABLED) {
      val ct2 = MCronTask(startDelay = DELETE_EMPTY_DIRS_START_DELAY, every = DELETE_EMPTY_DIRS_EVERY, displayName = EDD_CONF_PREFIX) {
        findAndDeleteEmptyDirsAsync().onFailure { case ex =>
          LOGGER.warn("Failed to findAndDeleteEmptyDirs()", ex)
        }
      }
      List(ct2)
    } else {
      Nil
    }
  }

  /** Выполнить в фоне всё необходимое. */
  def findAndDeleteEmptyDirsAsync(): Future[_] = {
    Future {
      findAndDeleteEmptyDirs()
    }(asyncUtil.singleThreadIoContext)
  }

  /** Пройтись по списку img-директорий, немножко заглянуть в каждую из них. */
  def findAndDeleteEmptyDirs(): Unit = {
    val dirStream = Files.newDirectoryStream( mLocalImgs.DIR.toPath )
    try {
      dirStream
        .iterator()
        .foreach { imgDirPath =>
          if (imgDirPath.toFile.isDirectory)
            maybeDeleteDirIfEmpty(imgDirPath)
        }
    } finally {
      dirStream.close()
    }
  }

  /** Удалить указанную директорию, если она пуста. */
  def maybeDeleteDirIfEmpty(dirPath: Path): Unit = {
    val dirStream = Files.newDirectoryStream(dirPath)

    val dirEmpty = try {
      dirStream.iterator().isEmpty
    } finally {
      dirStream.close()
    }

    if (dirEmpty) {
      try {
        LOGGER.debug("Deleting empty img-directory: " + dirPath)
        Files.delete(dirPath)
      } catch {
        case ex: Exception => LOGGER.warn("Unable to delete empty img directory: "  + dirPath, ex)
      }
    }
  }

}
