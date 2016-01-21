package models.im

import java.io.File
import java.nio.file.{Path, Files}
import java.util.UUID

import io.suggest.model.img.{ImgSzDated, IImgMeta}
import io.suggest.util.UuidUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.mcron.{ICronTask, MCronTask}
import models.mfs.FileUtil
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import play.api.Application
import play.api.Play.{current, configuration}
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import util._
import util.async.AsyncUtil
import util.img.{ImgFileNameParsersImpl, ImgFormUtil, OrigImageUtil}
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
  extends PlayLazyMacroLogsImpl
  with CronTasksProviderEmpty
  with PeriodicallyDeleteEmptyDirs
  with PeriodicallyDeleteNotExistingInPermanent
{

  // TODO DI
  private val mImg3 = current.injector.instanceOf[MImgs3]

  /** Реализация парсеров для filename из данной модели. */
  class Parsers extends ImgFileNameParsersImpl {

    override type T = MLocalImg

    /** Парсер имён файлов, конвертящий успешный результат своей работы в экземпляр MLocalImg. */
    override def fileName2miP: Parser[T] = {
      fileNameP ^^ {
        case uuid ~ dynArgs =>
          MLocalImg(uuid, dynArgs)
      }
    }

  }


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
    (new Parsers)
      .fromFileName(filename)
      .get
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
trait MLocalImgT extends MAnyImgT with PlayMacroLogsI {

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

  lazy val mimeMatchOptFut = {
    val fut = Future {
      FileUtil.getMimeMatch(file)
    }
    fut onFailure { case ex: Throwable =>
      LOGGER.error(s"Failed to get mime for file: $file [${file.length()} bytes]", ex)
    }
    fut
  }

  /** Определение mime-типа из файла. */
  lazy val mimeFut: Future[String] = for {
    mmOpt <- mimeMatchOptFut
  } yield {
    val mimeOpt = ImgFileUtil.getMime(mmOpt)
    ImgFileUtil.orUnknown( mimeOpt )
  }

  def fileExtensionFut: Future[String] = {
    mimeMatchOptFut map { mimeMatchOpt =>
      mimeMatchOpt.fold {
        LOGGER.warn("Mime match failed, guessing PNG extension")
        "png"
      } { mm =>
        mm.getExtension
      }
    }
  }

  def generateFileName: Future[String] = {
    for {
      fext <- fileExtensionFut
    } yield {
      rowKeyStr + "." + fext
    }
  }

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

  def prepareWriteFile(): Unit = {
    if (!fsImgDir.isDirectory)
      fsImgDir.delete()
    if (!fsImgDir.exists())
      fsImgDir.mkdirs()
  }

  // TODO Этот метод больше не используется, можно его удалить после окончания перепиливания картинок на N2-архитектуру.
  def writeIntoFile(imgBytes: Array[Byte]) {
    prepareWriteFile()
    FileUtils.writeByteArrayToFile(file, imgBytes)
  }

  def isExists: Boolean = file.exists()
  def deleteSync: Boolean = file.delete()

  override def delete: Future[_] = {
    Future {
      deleteSync
    }(AsyncUtil.singleThreadIoContext)
  }

  def imgBytesEnumerator: Enumerator[Array[Byte]] = {
    Enumerator.fromFile(file)
  }

  def identify = {
    Future {
      OrigImageUtil.identify(file)
    }(AsyncUtil.singleThreadCpuContext)
  }

  lazy val identifyCached = {
    Cache.getOrElse(fileName + ".iC", expiration = IDENTIFY_CACHE_TTL_SECONDS) {
      identify
    }
  }

  /** Асинхронно получить метаданные по этой картинке. */
  override lazy val getImageWH: Future[Option[MImgInfoMeta]] = {
    identifyCached
      .map { info =>
        val imeta = MImgInfoMeta(
          height = info.getImageHeight,
          width  = info.getImageWidth
        )
        Some(imeta)
      }
      .recover {
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

  override type MImg_t = MImgT

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

  override def original: MLocalImg = {
    if (dynImgOps.nonEmpty)
      copy(dynImgOps = Nil)
    else
      this
  }

  override lazy val toWrappedImg = MLocalImg.mImg3(rowKeyStr, dynImgOps)

  override lazy val rawImgMeta: Future[Option[IImgMeta]] = {
    if (isExists) {
      getImageWH map { metaOpt =>
        metaOpt map { meta =>
          ImgSzDated(meta, new DateTime(file.lastModified()))
        }
      }
    } else {
      Future successful None
    }
  }

  override lazy val cropOpt = super.cropOpt
}



/** Периодически стирать пустые директории через Cron. Это статический аддон к object MLocalImg.
  * Внутри, для работы с ФС, используется java.nio. */
trait PeriodicallyDeleteEmptyDirs extends ICronTasksProvider with PlayMacroLogsI {

  def DIR: File
  
  protected val EDD_CONF_PREFIX = "m.img.local.edd"

  /** Включено ли периодическое удаление пустых директорий из под картинок? */
  def DELETE_EMPTY_DIRS_ENABLED = configuration.getBoolean(EDD_CONF_PREFIX + ".enabled") getOrElse true

  /** Как часто инициировать проверку? */
  def DELETE_EMPTY_DIRS_EVERY = configuration.getInt(EDD_CONF_PREFIX + ".every.minutes")
    .fold [FiniteDuration] (12.hours) (_.minutes)

  /** На сколько отодвигать старт проверки. */
  def DELETE_EMPTY_DIRS_START_DELAY = configuration.getInt(EDD_CONF_PREFIX + ".start.delay.seconds")
    .getOrElse(60)
    .seconds

  /** Список задач, которые надо вызывать по таймеру. */
  abstract override def cronTasks(app: Application): TraversableOnce[ICronTask] = {
    val cts1 = super.cronTasks(app)
    if (DELETE_EMPTY_DIRS_ENABLED) {
      val ct2 = MCronTask(startDelay = DELETE_EMPTY_DIRS_START_DELAY, every = DELETE_EMPTY_DIRS_EVERY, displayName = EDD_CONF_PREFIX) {
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
trait PeriodicallyDeleteNotExistingInPermanent extends ICronTasksProvider with PlayMacroLogsI {

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
    .fold[FiniteDuration] (3.hours)(_.minutes)

  def DNEIP_OLD_DIR_AGE_MS: Long = configuration.getInt(DNEIP_CONF_PREFIX + ".old.age.minutes")
    .fold(2.hours)(_.minutes)
    .toMillis

  /** Список задач, которые надо вызывать по таймеру. */
  abstract override def cronTasks(app: Application): TraversableOnce[ICronTask] = {
    val cts0 = super.cronTasks(app)
    if (DNEIP_ENABLED) {
      val task = MCronTask(startDelay = DNEIP_START_DELAY, every = DNEIP_EVERY, displayName = DNEIP_CONF_PREFIX) {
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
          MLocalImg(rowKey)
            .toWrappedImg
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




