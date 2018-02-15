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
import org.apache.batik.gvt.GraphicsNode
import org.im4java.core.Info
import util.img.detect.main.MainColorDetector

import scala.concurrent.Future

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
trait IFileUpCtx {

  val filePart: MultipartFormData.FilePart[TemporaryFile]

  val mLocalImgOpt: Option[MLocalImg]

  def path: Path

  def file: File

  /** Задетекченный MIME-тип файла. */
  def detectedMimeType: Option[String]

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


/** Внутренний DI-контейнер для всей DI-утили, используемой внутри [[MFileUpCtx]]. */
protected class MFileUpCtxStatic @Inject() (
                                             val mLocalImgs                 : MLocalImgs,
                                             val imgFileUtil                : ImgFileUtil,
                                             val mainColorDetector          : MainColorDetector,
                                             //implicit val ec    : ExecutionContext
                                           )

/** Интерфейс для Guice DI-factory, которая собирает инстансы upload-контекста [[MFileUpCtx]]. */
trait IFileUpCtxFactory {
  def make( filePart     : MultipartFormData.FilePart[TemporaryFile],
            uploadArgs   : MUploadTargetQs,
            mLocalImg    : Option[MLocalImg]
          ): MFileUpCtx
}

/** Очевидная реализация [[IFileUpCtx]] на базе lazy vals. */
class MFileUpCtx(
                  @Assisted override val filePart       : MultipartFormData.FilePart[TemporaryFile],
                  @Assisted uploadArgs                  : MUploadTargetQs,
                  @Assisted override val mLocalImgOpt   : Option[MLocalImg],
                  statics                               : MFileUpCtxStatic
                )
  extends IFileUpCtx
{

  import statics._

  override val path = filePart.ref.path

  override val file = path.toFile

  override lazy val detectedMimeType: Option[String] = {
    MimeUtilJvm.probeContentType(path)
  }

  override lazy val imgFmtOpt = detectedMimeType.flatMap( MImgFmts.withMime )

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
      mimeType  <- detectedMimeType
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
