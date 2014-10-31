package models.im

import java.io.{FileFilter, File}
import java.nio.file.{Path, Files}
import java.util.UUID

import akka.actor.ActorContext
import io.suggest.event.SNStaticSubscriber
import io.suggest.event.SioNotifier.{Event, Subscriber, Classifier}
import io.suggest.event.subscriber.SnClassSubscriber
import io.suggest.model.{Img2FullyDeletedEvent, ImgWithTimestamp}
import io.suggest.util.UuidUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.{CronTask, ICronTask, ImgMetaI}
import org.apache.commons.io.FileUtils
import play.api.Play.{current, configuration}
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util._
import util.img.{ImgFileNameParsers, ImgFormUtil, OrigImageUtil}
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.10.14 16:29
 * Description: tmp-сущность модели MPictureTpl была сдвинута в сторону прослойки для системы orig-картинок,
 * уже сохранённых либо планируемых к сохранению в постоянном хранилище.
 * Также, произведена замена значения crop на цельный DynImgArgs. Маркер выкинут на помойку, т.к. в подмене
 * картинок нет смысла при использовании DynImg-подсистемы. Имя файла картинки теперь имеет вид,
 * очень похожий на ссылку:
 *   .../picture/tmp/aASDqefa4345szfvsedV_se4t-3?a=b&c=1123x543&....jpg
 *
 * В итоге, получилась эта модель.
 */

object MLocalImg
  extends ImgFileNameParsers
  with PlayLazyMacroLogsImpl
  with DeleteOnIm2FullyDeletedEvent
  with CronTasksProviderEmpty
  with PeriodicallyDeleteEmptyDirs
  with PeriodicallyDeleteNotExistingInPermanent
{

  /** Адрес img-директории, который используется для хранилища. */
  def DIR_REL = configuration.getString("m.local.img.dir.rel") getOrElse "picture/local"

  /** Экземпляр File, точно указывающий на директорию с данными этой модели. */
  val DIR = current.getFile(DIR_REL)

  /** Сколько модель должна кешировать в голове результат вызова identify. */
  val IDENTIFY_CACHE_TTL_SECONDS = configuration.getInt("m.local.img.identify.cache.ttl.seconds") getOrElse 120


  DIR.mkdirs()

  def getFsImgDir(rowKeyStr: String): File = new File(DIR, rowKeyStr)
  def getFsImgDir(rowKey: UUID): File = getFsImgDir( UuidUtil.uuidToBase64(rowKey) )

  /**
   * Получить экземпляр MLocalImg из img filename, в котором сериализована вся инфа по картинке.
   * @param filename Строка img filename.
   * @return Экземпляр MLocalImg или экзепшен.
   */
  def apply(filename: String): MLocalImg = {
    parseAll(fileName2mliP, filename).get
  }

  /** Парсер имён файлов, конвертящий успешный результат своей работы в экземпляр MLocalImg. */
  def fileName2mliP: Parser[MLocalImg] = {
    fileNameP ^^ {
      case uuid ~ dynArgs =>
        MLocalImg(uuid, dynArgs)
    }
  }

  /**
   * Стереть из файловой системы все упоминания картинок с указанным id.
   * На деле будет удалена директория с указанным именем.
   * @param rowKey id картинок.
   */
  def deleteAllSyncFor(rowKey: UUID): Unit = {
    val dir = getFsImgDir(rowKey)
    FileUtils.deleteDirectory(dir)
  }

  /**
   * Асинхронно стереть все картинки, у которых указанный id.
   * По сути неблокирующий враппер над deleteAllSyncFor().
   * @param rowKey id картинок.
   * @return Фьючерс для синхронизации.
   */
  def deleteAllFor(rowKey: UUID): Future[_] = {
    Future {
      deleteAllSyncFor(rowKey)
    }(AsyncUtil.singleThreadIoContext)
  }

}




/** Трейт, выносящий часть функционала экземпляра, на случай дальнейших расширений и разделений. */
trait MLocalImgT extends ImgWithTimestamp with PlayMacroLogsI with MAnyImgT {

  import MLocalImg._

  // Для организации хранения файлов используется rowKey/qs-структура.
  lazy val fsImgDir = getFsImgDir(rowKeyStr)

  lazy val fsFileName: String = {
    if (hasImgOps) {
      dynImgOpsString
    } else {
      "__ORIG__"
    }
  }
  
  lazy val file = new File(fsImgDir, fsFileName)

  /** 
   * Принудительно в фоне обновляем file last modified time.
   * Обычно atime на хосте отключён или переключен в relatime, а этим временем пользуется чистильщик ненужных картинок.
   * @param newMtime Новое время доступа к картинке.
   * @return Фьючерс для синхронизации.
   */
  def touchAsync(newMtime: Long = System.currentTimeMillis()): Future[_] = {
    Future {
      file.setLastModified(newMtime)
    }(AsyncUtil.singleThreadIoContext)
  }

  def writeIntoFile(imgBytes: Array[Byte]) {
    if (!fsImgDir.isDirectory)
      fsImgDir.delete()
    if (!fsImgDir.exists())
      fsImgDir.mkdirs()
    FileUtils.writeByteArrayToFile(file, imgBytes)
  }

  def isExists: Boolean = file.exists()
  def deleteSync: Boolean = file.delete()

  override def delete: Future[_] = {
    Future {
      deleteSync
    }(AsyncUtil.singleThreadIoContext)
  }

  override def timestampMs: Long = file.lastModified

  override def imgBytes: Array[Byte] = Files.readAllBytes(file.toPath)

  def identify = {
    Future {
      val info = OrigImageUtil.identify(file)
      val imeta = MImgInfoMeta(
        height = info.getImageHeight,
        width = info.getImageWidth
      )
      Some(imeta)
    }(AsyncUtil.singleThreadCpuContext)
  }

  lazy val identifyCached = {
    Cache.getOrElse(fileName + ".iC", expiration = IDENTIFY_CACHE_TTL_SECONDS) {
      identify
    }
  }

  /** Асинхронно получить метаданные по этой картинке. */
  override lazy val getImageWH: Future[Option[MImgInfoMeta]] = {
    identifyCached recover {
      case ex: org.im4java.core.InfoException =>
        LOGGER.info("getImageWH(): Unable to identity image " + fileName, ex)
        None
    }
  }

  def imgMdMap: Future[Option[Map[String, String]]] = {
    getImageWH map { metaOpt =>
      metaOpt map { meta =>
        Map(
          ImgFormUtil.IMETA_HEIGHT -> meta.height.toString,
          ImgFormUtil.IMETA_WIDTH  -> meta.width.toString
        )
      }
    }
  }

}


/**
 * Экземпляр класса локально хранимой картинки в ФС.
 * @param rowKey Ключ (id картинки).
 * @param dynImgOps IM-операции, которые нужно наложить на оригинал с ключом rowKey, чтобы получить
 *                  необходимою картинку.
 */
case class MLocalImg(
  rowKey      : UUID = UUID.randomUUID(),
  dynImgOps   : Seq[ImOp] = Nil
) extends MLocalImgT with PlayLazyMacroLogsImpl {

  override lazy val rowKeyStr: String = UuidUtil.uuidToBase64(rowKey)

  override def hasImgOps: Boolean = dynImgOps.nonEmpty

  override lazy val dynImgOpsString: String = super.dynImgOpsString

  override lazy val fileName = super.fileName

  override def toLocalImg: Future[Option[MLocalImgT]] = {
    val result = if (isExists)
      Some(this)
    else
      None
    Future successful result
  }

  override def original = {
    if (dynImgOps.nonEmpty)
      copy(dynImgOps = Nil)
    else
      this
  }

  override lazy val toWrappedImg = MImg(rowKey, dynImgOps)

  override lazy val rawImgMeta: Future[Option[ImgMetaI]] = {
    if (isExists) {
      imgMdMap.map { mdMapOpt =>
        mdMapOpt.map { mdMap =>
          new ImgMetaI {
            override lazy val md = mdMap
            override lazy val timestampMs = file.lastModified
          }
        }
      }
    } else {
      Future successful None
    }
  }

  override lazy val cropOpt = super.cropOpt
}


/** Статический аддон для добавления поддержки удаления локальных картинок по событию удаления картинки
  * из permanent-хранилища. */
trait DeleteOnIm2FullyDeletedEvent extends SNStaticSubscriber with SnClassSubscriber with PlayMacroLogsI {

  val DIR: File
  def deleteAllFor(rowKey: UUID): Future[_]

  override def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    List(Img2FullyDeletedEvent.getClassifier() -> Seq(this))
  }

  override def publish(event: Event)(implicit ctx: ActorContext): Unit = {
    event match {
      case e @ Img2FullyDeletedEvent(rowKey) =>
        deleteAllFor(rowKey) onFailure {
          case ex =>
            LOGGER.error(classOf[DeleteOnIm2FullyDeletedEvent].getSimpleName +
              ": Failed to delete locel img with key " + e.rowKeyStr, ex)
        }

      case other =>
        LOGGER.warn("Unexpected event received: " + other)
    }
  }

}


/** Периодически стирать пустые директории через Cron. Это статический аддон к object MLocalImg.
  * Внутри, для работы с ФС, используется java.nio. */
trait PeriodicallyDeleteEmptyDirs extends CronTasksProvider with PlayMacroLogsI {

  def DIR: File
  
  protected val EDD_CONF_PREFIX = "m.img.local.edd"

  /** Включено ли периодическое удаление пустых директорий из под картинок? */
  def DELETE_EMPTY_DIRS_ENABLED = configuration.getBoolean(EDD_CONF_PREFIX + ".enabled") getOrElse true

  /** Как часто инициировать проверку? */
  def DELETE_EMPTY_DIRS_EVERY = configuration.getInt(EDD_CONF_PREFIX + ".every.minutes")
    .fold [FiniteDuration] (12 hours) (_ minutes)

  /** На сколько отодвигать старт проверки. */
  def DELETE_EMPTY_DIRS_START_DELAY = configuration.getInt(EDD_CONF_PREFIX + ".start.delay.seconds")
    .getOrElse(60)
    .seconds

  /** Список задач, которые надо вызывать по таймеру. */
  abstract override def cronTasks: TraversableOnce[ICronTask] = {
    val cts1 = super.cronTasks
    if (DELETE_EMPTY_DIRS_ENABLED) {
      val ct2 = CronTask(startDelay = DELETE_EMPTY_DIRS_START_DELAY, every = DELETE_EMPTY_DIRS_EVERY, displayName = EDD_CONF_PREFIX) {
        findAndDeleteEmptyDirsAsync onFailure { case ex =>
          LOGGER.warn("Failed to findAndDeleteEmptyDirs()", ex)
        }
      }
      List(ct2).iterator ++ cts1.toIterator
    } else {
      cts1
    }
  }

  /** Выполнить в фоне всё необходимое. */
  def findAndDeleteEmptyDirsAsync: Future[_] = {
    Future {
      findAndDeleteEmptyDirs
    }(AsyncUtil.singleThreadIoContext)
  }

  /** Пройтись по списку img-директорий, немножко заглянуть в каждую из них. */
  def findAndDeleteEmptyDirs: Unit = {
    val dirStream = Files.newDirectoryStream(DIR.toPath)
    try {
      dirStream.iterator()
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


/** Надо периодичеки удалять директории с картинками, если они долго лежат,
  * а в permanent ещё/уже нет картинок с данным id. */
trait PeriodicallyDeleteNotExistingInPermanent extends CronTasksProvider with PlayMacroLogsI {

  def DIR: File

  protected val DNEIP_CONF_PREFIX = "m.img.local.dneip"

  /** Включено ли автоудаление директорий? */
  def DNEIP_ENABLED = configuration.getBoolean(DNEIP_CONF_PREFIX + ".enabled") getOrElse true

  /** Задержка первого запуска после старта play. */
  def DNEIP_START_DELAY = configuration.getInt(DNEIP_CONF_PREFIX + ".start.delay.seconds")
    .getOrElse(15)
    .seconds

  /** Как часто проводить проверки? */
  def DNEIP_EVERY = configuration.getInt(DNEIP_CONF_PREFIX + ".every.minutes")
    .fold[FiniteDuration] (3 hours)(_ minutes)

  def DNEIP_OLD_DIR_AGE_MS: Long = configuration.getInt(DNEIP_CONF_PREFIX + ".old.age.minutes")
    .fold(2 hours)(_ minutes)
    .toMillis

  /** Список задач, которые надо вызывать по таймеру. */
  abstract override def cronTasks: TraversableOnce[ICronTask] = {
    val cts0 = super.cronTasks
    if (DNEIP_ENABLED) {
      val task = CronTask(startDelay = DNEIP_START_DELAY, every = DNEIP_EVERY, displayName = DNEIP_CONF_PREFIX) {
        dneipFindAndDeleteAsync() onFailure { case ex =>
          LOGGER.error("DNEIP: Clean-up failed.", ex)
        }
      }
      Seq(task).iterator ++ cts0.toIterator
    } else {
      cts0
    }
  }

  def dneipFindAndDeleteAsync(): Future[_] = {
    Future {
      dneipFindAndDelete()
    }
  }

  def dneipFindAndDelete(): Unit = {
    val dirStream = Files.newDirectoryStream(DIR.toPath)
    try {
      val oldNow = System.currentTimeMillis() - DNEIP_OLD_DIR_AGE_MS
      dirStream.iterator()
        .map(_.toFile)
        // TODO Частые проверки должны отрабатывать только свежие директории. Редкие - все директории.
        .filter { f  =>  f.isDirectory && f.lastModified() < oldNow }
        .foreach { currDir =>
          val rowKeyStr = currDir.getName
          val rowKey = UuidUtil.base64ToUuid(rowKeyStr)
          MLocalImg(rowKey).toWrappedImg
            .existsInPermanent
            .filter(!_)
            .andThen { case _: Success[_] =>
              LOGGER.debug("Deleting permanent-less img-dir: " + currDir)
              FileUtils.deleteDirectory(currDir)
            }(AsyncUtil.singleThreadIoContext)
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




