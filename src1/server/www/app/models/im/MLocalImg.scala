package models.im

import java.io.File
import java.time.{Instant, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import io.suggest.async.AsyncUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.model.img.ImgSzDated
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import net.sf.jmimemagic.MagicMatch
import org.apache.commons.io.FileUtils
import util.img.{ImgFileNameParsersImpl, ImgFileUtil, OrigImageUtil}
import util.up.FileUtil

import scala.concurrent.duration._
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

@Singleton
class MLocalImgs @Inject() (
  origImageUtil : OrigImageUtil,
  imgFileUtil   : ImgFileUtil,
  asyncUtil     : AsyncUtil,
  fileUtil      : FileUtil,
  mCommonDi     : ICommonDi
)
  extends MAnyImgsT[MLocalImg]
  with MacroLogsImpl
{

  import mCommonDi._


  /** Сколько модель должна кешировать в голове результат вызова identify? */
  private def IDENTIFY_CACHE_TTL_SECONDS = 120

  /** Адрес img-директории, который используется для хранилища. */
  private def DIR_REL = configuration.getOptional[String]("m.local.img.dir.rel")
    .getOrElse("picture/local")

  /** Экземпляр File, точно указывающий на директорию с данными этой модели. */
  def DIR = current.environment.getFile(DIR_REL)

  DIR.mkdirs()

  def getFsImgDir(mimg: MLocalImg): File    = getFsImgDir(mimg.rowKeyStr)
  def getFsImgDir(rowKeyStr: String): File  = new File(DIR, rowKeyStr)


  def deleteSync(mimg: MLocalImg): Boolean = {
    fileOf(mimg).delete()
  }

  override def delete(mimg: MLocalImg): Future[_] = {
    Future {
      deleteSync(mimg)
    }(asyncUtil.singleThreadIoContext)
  }

  override def toLocalImg(mimg: MLocalImg): Future[Option[MLocalImg]] = {
    val result = if (isExists(mimg))
      Some(mimg.toLocalInstance)
    else
      None
    Future.successful(result)
  }

  /**
   * Принудительно в фоне обновляем file last modified time.
   * Обычно atime на хосте отключён или переключен в relatime, а этим временем пользуется чистильщик ненужных картинок.
   * @return Фьючерс для синхронизации.
   */
  def touchAsync(mimg: MLocalImg): Future[_] = {
    Future {
      val tms = System.currentTimeMillis()
      val file = fileOf(mimg)
      file.setLastModified(tms)
    }(asyncUtil.singleThreadIoContext)
  }

  override def getStream(mimg: MLocalImg): Source[ByteString, _] = {
    val file = fileOf(mimg)
    FileIO.fromPath( file.toPath )
  }

  def identify(mimg: MLocalImg) = {
    Future {
      val file = fileOf(mimg)
      origImageUtil.identify(file)
    }(asyncUtil.singleThreadCpuContext)
  }

  def identifyCached(mimg: MLocalImg) = {
    cacheApiUtil.getOrElseFut(mimg.fileName + ".identify", IDENTIFY_CACHE_TTL_SECONDS.seconds) {
      identify(mimg)
    }
  }

  /** Получить ширину и длину картинки. */
  override def getImageWH(mimg: MLocalImg): Future[Option[MSize2di]] = {
    identifyCached(mimg)
      .map { info =>
        val imeta = imgFileUtil.identityInfo2wh(info)
        Some(imeta)
      }
      .recover {
        case ex: org.im4java.core.InfoException =>
          LOGGER.info("getImageWH(): Unable to identity image " + mimg.fileName, ex)
          None
      }
  }

  override def rawImgMeta(mimg: MLocalImg): Future[Option[ImgSzDated]] = {
    if (isExists(mimg)) {
      for (metaOpt <- getImageWH(mimg)) yield {
        for (meta <- metaOpt) yield {
          val file = fileOf(mimg)
          val dt = Instant
            .ofEpochMilli( file.lastModified() )
            .atOffset( ZoneOffset.UTC )
          ImgSzDated(meta, dt)
        }
      }
    } else {
      Future.successful( None )
    }
  }

  /**
   * Стереть из файловой системы все упоминания картинок с указанным id.
   * На деле будет удалена директория с указанным именем.
   * @param rowKeyStr id узла картинки.
   */
  def deleteAllSyncFor(rowKeyStr: String): Unit = {
    val dir = getFsImgDir(rowKeyStr)
    FileUtils.deleteDirectory(dir)
  }

  /**
   * Асинхронно стереть все картинки, у которых указанный id.
   * По сути неблокирующий враппер над deleteAllSyncFor().
   * @param rowKeyStr Основной id узла-картинки.
   * @return Фьючерс для синхронизации.
   */
  def deleteAllFor(rowKeyStr: String): Future[_] = {
    Future {
      deleteAllSyncFor(rowKeyStr)
    }(asyncUtil.singleThreadIoContext)
  }

  /** Подготовка к записи в файл указанного локального изображения. */
  def prepareWriteFile(mimg: MLocalImg): Unit = {
    val _fsImgDir = getFsImgDir(mimg)
    if (!_fsImgDir.isDirectory)
      _fsImgDir.delete()
    if (!_fsImgDir.exists())
      _fsImgDir.mkdirs()
  }

  def fileExtensionFut(mimg: MLocalImg): Future[String] = {
    for (mimeMatchOpt <- mimeMatchOptFut(mimg)) yield {
      mimeMatchOpt.fold {
        LOGGER.warn("Mime match failed, guessing PNG extension")
        "png"
      } { mm =>
        mm.getExtension
      }
    }
  }

  def mimeMatchOptFut(mimg: MLocalImg): Future[Option[MagicMatch]] = {
    val file = fileOf(mimg)
    val fut = Future {
      fileUtil.getMimeMatch(file)
    }
    for (ex <- fut.failed) {
      LOGGER.error(s"Failed to get mime for file: $file [${file.length()} bytes]", ex)
    }
    fut
  }

  /** Определение mime-типа из файла. */
  def mimeFut(mimg: MLocalImg): Future[String] = {
    for {
      mmOpt <- mimeMatchOptFut(mimg)
    } yield {
      val mimeOpt = imgFileUtil.getMime(mmOpt)
      imgFileUtil.orUnknown( mimeOpt )
    }
  }

  def generateFileName(mimg: MLocalImg): Future[String] = {
    for (fext <- fileExtensionFut(mimg)) yield {
      mimg.rowKeyStr + "." + fext
    }
  }

  def isExists(mimg: MLocalImg): Boolean = {
    fileOf(mimg).exists()
  }

  def fileOf(mimg: MLocalImg): File = {
    val fsImgDir = getFsImgDir(mimg)
    new File(fsImgDir, mimg.fsFileName)
  }

}


/** Интерфейс для поля с DI-инстансом [[MLocalImgs]]. */
trait IMLocalImgs {
  def mLocalImgs: MLocalImgs
}



/** Вообще полная статика модели [[MLocalImg]]. */
object MLocalImg {

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

}


/**
 * Экземпляр класса локально хранимой картинки в ФС.
 * @param rowKeyStr Ключ (id узла-картинки).
 * @param dynImgOps IM-операции, которые нужно наложить на оригинал с ключом rowKey, чтобы получить
 *                  необходимою картинку.
 */
case class MLocalImg(
                      rowKeyStr   : String    = UuidUtil.uuidToBase64( UUID.randomUUID() ),
                      dynImgOps   : Seq[ImOp] = Nil
                    )
  extends MAnyImgT
{

  override type MImg_t = MImgT

  override def toLocalInstance: MLocalImg = this

  override def original: MLocalImg = {
    if (hasImgOps)
      copy(dynImgOps = Nil)
    else
      this
  }

  override lazy val toWrappedImg: MImg3 = {
    MImg3(rowKeyStr, dynImgOps)
  }

  override lazy val cropOpt = super.cropOpt

  lazy val fsFileName: String = {
    if (hasImgOps) {
      dynImgOpsString
    } else {
      "__ORIG__"
    }
  }

}
