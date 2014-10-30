package models.im

import java.io.File
import java.nio.file.Files
import java.util.UUID

import io.suggest.model.ImgWithTimestamp
import io.suggest.util.UuidUtil
import io.suggest.ym.model.common.MImgInfoMeta
import models.ImgMetaI
import org.apache.commons.io.FileUtils
import play.api.Play.{current, configuration}
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.{PlayLazyMacroLogsImpl, PlayMacroLogsI, AsyncUtil}
import util.img.{ImgFileNameParsers, ImgFormUtil, OrigImageUtil}

import scala.concurrent.Future

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

object MLocalImg extends ImgFileNameParsers {

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
    }(AsyncUtil.singleThreadBlockingContext)
  }

}


import MLocalImg._


/** Трейт, выносящий часть функционала экземпляра, на случай дальнейших расширений и разделений. */
trait MLocalImgT extends ImgWithTimestamp with PlayMacroLogsI with MAnyImgT with PlayLazyMacroLogsImpl {
  
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

  def touch(newLastMod: Long = System.currentTimeMillis()) = {
    file.setLastModified(newLastMod)
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
    }(AsyncUtil.singleThreadBlockingContext)
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
    }(AsyncUtil.extCpuHeavyContext)
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

