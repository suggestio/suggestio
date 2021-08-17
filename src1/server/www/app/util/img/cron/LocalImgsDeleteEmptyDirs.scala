package util.img.cron

import java.nio.file.{Files, Path}
import javax.inject.Inject
import io.suggest.async.AsyncUtil
import io.suggest.util.logs.MacroLogsImpl
import models.im.MLocalImgsConf
import models.mcron.MCronTask
import play.api.inject.Injector
import util.cron.ICronTasksProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.16 14:09
  * Description: Периодически стирать пустые директории через Cron.
  * Внутри, для работы с ФС, используется java.nio.
  */
class LocalImgsDeleteEmptyDirs @Inject()(
                                          injector      : Injector,
                                        )
  extends ICronTasksProvider
  with MacroLogsImpl
{

  private def mLocalImgsConf = injector.instanceOf[MLocalImgsConf]
  private def asyncUtil = injector.instanceOf[AsyncUtil]
  implicit def ec = injector.instanceOf[ExecutionContext]


  /** Как часто инициировать проверку? */
  private def DELETE_EMPTY_DIRS_EVERY = 12.hours

  /** На сколько отодвигать старт проверки. */
  private def DELETE_EMPTY_DIRS_START_DELAY = 60.seconds

  private def _deleteEmptyImgsTask = MCronTask(
    startDelay  = DELETE_EMPTY_DIRS_START_DELAY,
    every       = DELETE_EMPTY_DIRS_EVERY,
    displayName = "m.img.local.edd",
  ) { () =>
    for (ex <- findAndDeleteEmptyDirsAsync().failed)
      LOGGER.warn("cronTasks(): Failed to findAndDeleteEmptyDirs()", ex)
  }

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): Iterable[MCronTask] = {
    _deleteEmptyImgsTask #::
    LazyList.empty
  }

  /** Выполнить в фоне всё необходимое. */
  def findAndDeleteEmptyDirsAsync(): Future[_] = {
    Future {
      findAndDeleteEmptyDirs()
    }(asyncUtil.singleThreadIoContext)
  }

  /** Пройтись по списку img-директорий, немножко заглянуть в каждую из них. */
  def findAndDeleteEmptyDirs(): Unit = {
    val dirStream = Files.newDirectoryStream( mLocalImgsConf.DIR.toPath )
    try {
      dirStream
        .iterator()
        .asScala
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
      dirStream.iterator()
        .asScala
        .isEmpty
    } finally {
      dirStream.close()
    }

    lazy val logPrefix = s"maybeDeleteDirIfEmpty($dirPath):"

    if (dirEmpty) {
      try {
        LOGGER.trace(s"$logPrefix Delete empty img-directory.")
        Files.delete(dirPath)
      } catch {
        case ex: Exception =>
          LOGGER.warn(s"$logPrefix Unable to delete empty img directory", ex)
      }
    }
  }

}
