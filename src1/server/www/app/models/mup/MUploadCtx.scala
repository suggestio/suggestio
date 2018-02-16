package models.mup

import java.io.File
import java.nio.file.Path
import javax.inject.Inject

import com.google.inject.assistedinject.Assisted
import io.suggest.color.MHistogram
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MimeUtilJvm
import io.suggest.img.{MImgFmt, MImgFmts}
import io.suggest.svg.SvgUtil
import models.im.{MLocalImg, MLocalImgs}
import org.w3c.dom.Document
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import util.img.ImgFileUtil
import io.suggest.common.geom.d2.MSize2diJvm.Implicits._
import io.suggest.model.n2.media.MFileMetaHash
import org.apache.batik.gvt.GraphicsNode
import org.im4java.core.Info
import util.img.detect.main.MainColorDetector
import util.up.FileUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.18 21:24
  * Description: Интерфейс модели контекста аплоада одного файла.
  * Сама реализация живёт рядом и завязана на DI-factory, т.к. поля модели подразумевают
  * только существование уровня контроллера.
  *
  * Здесь интефрейсятся поля результатов операций над загруженным файлом,
  * и которые могут быть расшарены между разными шагами сложной логики или разными компонентами,
  * занятыми в процессе отработки аплоада.
  */
trait IUploadCtx {

  val filePart: MultipartFormData.FilePart[TemporaryFile]

  val mLocalImgOpt: Option[MLocalImg]

  def path: Path

  def file: File

  def fileLength: Long

  def declaredMime: String

  /** Задетекченный MIME-тип файла. */
  def detectedMimeTypeOpt: Option[String]

  /** Парралельный рассчёт всех интересующих хешей загруженного файла. БЕЗ сравнивания с оригиналами. */
  def hashesHexFut: Future[Iterable[MFileMetaHash]]

  /** Формат картинки, если загруженный файл -- это картинка в поддерживаемом формате. */
  def imgFmtOpt: Option[MImgFmt]

  def isImg: Boolean = imgFmtOpt.nonEmpty

  /** Парсинг файла как svg-документа. */
  def svgDocOpt: Option[Document]

  def svgGvtOpt: Option[GraphicsNode]

  /** Ширина и длина картинки, когда это картинка. */
  def imageWh: Option[MSize2di]

  def identifyInfoOpt: Option[Future[Info]]

  /** Результат выполнения color-detect'ора над картинкой. */
  def colorDetectOptFut: Option[Future[MHistogram]]

}


/** Внутренний DI-контейнер для всей DI-утили, используемой внутри [[MUploadCtx]]. */
protected class MUploadCtxStatic @Inject()(
                                            val mLocalImgs                 : MLocalImgs,
                                            val imgFileUtil                : ImgFileUtil,
                                            val mainColorDetector          : MainColorDetector,
                                            val fileUtil                   : FileUtil,
                                            implicit val ec                : ExecutionContext
                                          )

/** Интерфейс для Guice DI-factory, которая собирает инстансы upload-контекста [[MUploadCtx]]. */
trait IUploadCtxFactory {
  def make( filePart     : MultipartFormData.FilePart[TemporaryFile],
            uploadArgs   : MUploadTargetQs,
            mLocalImg    : Option[MLocalImg]
          ): MUploadCtx
}

/** Очевидная реализация [[IUploadCtx]] на базе lazy vals. */
class MUploadCtx @Inject() (
                             @Assisted override val filePart       : MultipartFormData.FilePart[TemporaryFile],
                             @Assisted uploadArgs                  : MUploadTargetQs,
                             @Assisted override val mLocalImgOpt   : Option[MLocalImg],
                             statics                               : MUploadCtxStatic
                           )
  extends IUploadCtx
{

  import statics._

  override val path = filePart.ref.path

  override val file = path.toFile

  override lazy val fileLength = file.length()

  override def declaredMime = uploadArgs.fileProps.mimeType

  override lazy val detectedMimeTypeOpt: Option[String] = {
    MimeUtilJvm.probeContentType(path)
  }

  override lazy val hashesHexFut: Future[Iterable[MFileMetaHash]] = {
    val origHashesFlags = Set( MFileMetaHash.Flags.TRULY_ORIGINAL )
    Future.traverse( uploadArgs.fileProps.hashesHex ) {
      case (mhash, _) =>
        for {
          srcHash <- Future {
            fileUtil.mkFileHash(mhash, file)
          }
        } yield {
          MFileMetaHash(mhash, srcHash, origHashesFlags)
        }
    }
  }

  override lazy val imgFmtOpt = detectedMimeTypeOpt.flatMap( MImgFmts.withMime )

  override lazy val svgDocOpt: Option[Document] = {
    SvgUtil.safeOpenWrap(
      SvgUtil.open(file)
    )
  }

  override lazy val svgGvtOpt: Option[GraphicsNode] = {
    for {
      svgDoc <- svgDocOpt
    } yield {
      SvgUtil.buildGvt(svgDoc)
    }
  }

  /** Ширина и длина картинки, когда это картинка. */
  override lazy val imageWh: Option[MSize2di] = {
    for {
      imgFmt    <- imgFmtOpt
      mimeType  <- detectedMimeTypeOpt
    } yield {
      imgFmt match {
        case MImgFmts.PNG | MImgFmts.JPEG | MImgFmts.GIF =>
          imgFileUtil.getImageWh( mimeType, file )
        case MImgFmts.SVG =>
          svgGvtOpt
            .get
            .getBounds
            .toSize2di
      }
    }
  }

  override lazy val identifyInfoOpt: Option[Future[Info]] = {
    for (mimg <- mLocalImgOpt) yield {
      mLocalImgs.identifyCached( mimg )
    }
  }

  /** Результат выполнения color-detect'ора над картинкой. */
  override lazy val colorDetectOptFut: Option[Future[MHistogram]] = {
    for {
      mimg        <- mLocalImgOpt
      cdArgs      <- uploadArgs.colorDetect
    } yield {
      mainColorDetector.cached(mimg) {
        mainColorDetector.detectPaletteFor(mimg, maxColors = cdArgs.paletteSize)
      }
    }
  }

}
