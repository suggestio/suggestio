package models.im

import java.io.File
import java.time.{Instant, ZoneOffset}

import javax.inject.{Inject, Singleton}
import akka.stream.scaladsl.FileIO
import io.suggest.async.AsyncUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MimeUtilJvm
import io.suggest.fio.IDataSource
import io.suggest.img
import io.suggest.img.ImgSzDated
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import org.apache.commons.io.FileUtils
import org.im4java.core.Info
import util.img.ImgFileUtil

import scala.concurrent.duration._
import scala.concurrent.blocking
import scala.concurrent.Future
import scala.util.Try

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
                             imgFileUtil                 : ImgFileUtil,
                             asyncUtil                   : AsyncUtil,
                             override val mCommonDi      : ICommonDi
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

  def getFsImgDir(mimg: MLocalImg): File    = getFsImgDir(mimg.dynImgId.origNodeId)
  def getFsImgDir(rowKeyStr: String): File  = new File(DIR, rowKeyStr)


  def deleteSync(mimg: MLocalImg): Boolean = {
    val f = fileOf(mimg)
    blocking( f.delete() )
  }

  override def delete(mimg: MLocalImg): Future[_] = {
    Future {
      deleteSync(mimg)
    }(asyncUtil.singleThreadIoContext)
  }

  override def toLocalImg(mimg: MLocalImg): Future[Option[MLocalImg]] = {
    val result = Option.when( isExists(mimg) )(
      mimg.toLocalInstance
    )
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
      blocking( file.setLastModified(tms) )
    }(asyncUtil.singleThreadIoContext)
  }

  override def getDataSource(mimg: MLocalImg): Future[IDataSource] = {
    val file = fileOf(mimg)
    val ds = new IDataSource {
      override lazy val data        = FileIO.fromPath( file.toPath )
      override lazy val sizeB       = blocking( file.length() )
      override lazy val contentType = getMimeSync(mimg)
      // Без скрытого сжатия, тут его не бывает.
      override def compression = None
    }
    Future.successful(ds)
  }

  def identify(mimg: MLocalImg) = {
    Future {
      val file = fileOf(mimg)
      if ( blocking(file.exists()) ) {
        val fmtAndPath = mimg.dynImgId.dynFormat.imFormat + ":" + file.getAbsolutePath
        blocking( new Info(fmtAndPath, true) )

      } else
        throw new NoSuchElementException("identify(): File is missing: " + file)
    }(asyncUtil.singleThreadCpuContext)
  }

  def identifyCached(mimg: MLocalImg) = {
    cacheApiUtil.getOrElseFut(mimg.dynImgId.fileName + ".identify", IDENTIFY_CACHE_TTL_SECONDS.seconds) {
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
          LOGGER.info("getImageWH(): Unable to identity image " + mimg.dynImgId.fileName, ex)
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
          img.ImgSzDated(meta, dt)
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
    blocking {
      FileUtils.deleteDirectory(dir)
    }
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
    blocking {
      if (!_fsImgDir.isDirectory)
        _fsImgDir.delete()
      if (!_fsImgDir.exists())
        _fsImgDir.mkdirs()
    }
  }

  def getMimeOptSync(mimg: MLocalImg): Option[String] = {
    val file = fileOf(mimg)
    val tryRes = Try(
      MimeUtilJvm.probeContentType( file.toPath )
    )
    if (tryRes.isFailure)
      LOGGER.error(s"Failed to get mime for file: $file [${file.length()} bytes]", tryRes.failed.get)
    tryRes
      .toOption
      .flatten
  }
  def getMimeSync(mimg: MLocalImg): String = {
    imgFileUtil.orUnknown( getMimeOptSync(mimg) )
  }

  /** Определение mime-типа из файла. */
  def mimeFut(mimg: MLocalImg): Future[String] = {
    Future {
      getMimeSync(mimg)
    }
  }


  def generateFileName(mimg: MLocalImg): String = {
    mimg.dynImgId.origNodeId + "." + mimg.dynImgId.dynFormat.fileExt
  }

  def isExists(mimg: MLocalImg): Boolean = {
    val f = fileOf(mimg)
    blocking( f.exists() )
  }

  def fileOf(mimg: MLocalImg): File = {
    val fsImgDir = getFsImgDir(mimg)
    new File(fsImgDir, mimg.dynImgId.fsFileName)
  }

}


/** Интерфейс для поля с DI-инстансом [[MLocalImgs]]. */
trait IMLocalImgs {
  def mLocalImgs: MLocalImgs
}



/** Экземпляр класса локально хранимой картинки в ФС. */
case class MLocalImg(
                      override val dynImgId    : MDynImgId
                    )
  extends MAnyImgT
{

  // TODO Почему тут MImgT?
  override type MImg_t = MImgT

  override def toLocalInstance: MLocalImg = this

  // TODO Оригинал ли будет на выходе? Что с форматом?
  override def original: MLocalImg = {
    if (dynImgId.hasImgOps)
      withDynImgId( dynImgId.original )
    else
      this
  }

  override lazy val toWrappedImg: MImg3 = {
    MImg3(dynImgId)
  }

  def withDynImgId(dynImgId: MDynImgId) = copy(dynImgId = dynImgId)

}
