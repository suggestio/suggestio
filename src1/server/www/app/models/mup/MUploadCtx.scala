package models.mup

import java.awt.image.BufferedImage

import javax.inject.Inject
import com.google.inject.assistedinject.Assisted
import io.suggest.color.MHistogram
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MimeUtilJvm
import io.suggest.img.MImgFmts
import io.suggest.svg.SvgUtil
import models.im.{MLocalImg, MLocalImgs}
import org.w3c.dom.Document
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import util.img.ImgFileUtil
import io.suggest.common.geom.d2.MSize2diJvm.Implicits._
import io.suggest.n2.media.{MFileMetaHash, MFileMetaHashFlag, MFileMetaHashFlags}
import io.suggest.util.logs.MacroLogsImpl
import org.apache.batik.gvt.GraphicsNode
import org.im4java.core.Info
import util.img.detect.main.MainColorDetector
import util.up.FileUtil
import japgolly.univeq._

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

/** Внутренний DI-контейнер для всей DI-утили, используемой внутри [[MUploadCtx]]. */
protected class MUploadCtxStatic @Inject()(
                                            val mLocalImgs                 : MLocalImgs,
                                            val imgFileUtil                : ImgFileUtil,
                                            val mainColorDetector          : MainColorDetector,
                                            val fileUtil                   : FileUtil,
                                            implicit val ec                : ExecutionContext
                                          )
  extends MacroLogsImpl


/** Интерфейс для Guice DI-factory, которая собирает инстансы upload-контекста [[MUploadCtx]]. */
trait IUploadCtxFactory {
  def make( filePart     : MultipartFormData.FilePart[TemporaryFile],
            uploadArgs   : MUploadTargetQs,
            mLocalImg    : Option[MLocalImg]
          ): MUploadCtx
}

/** Реализация upload-контекста на базе lazy vals. */
class MUploadCtx @Inject() (
                             @Assisted val filePart       : MultipartFormData.FilePart[TemporaryFile],
                             @Assisted uploadArgs         : MUploadTargetQs,
                             @Assisted val mLocalImgOpt   : Option[MLocalImg],
                             statics                      : MUploadCtxStatic
                           ) {

  import statics._

  lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

  val path = filePart.ref.path

  val file = path.toFile

  lazy val fileLength = file.length()

  def declaredMime = uploadArgs.fileProps.mimeType

  lazy val detectedMimeTypeOpt: Option[String] = {
    MimeUtilJvm.probeContentType(path)
  }

  lazy val hashesHexFut: Future[Seq[MFileMetaHash]] = {
    val origHashesFlags = Set[MFileMetaHashFlag]( MFileMetaHashFlags.TrulyOriginal )
    Future.traverse( uploadArgs.fileProps.hashesHex.toSeq ) {
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

  lazy val imgFmtOpt = detectedMimeTypeOpt.flatMap( MImgFmts.withMime )

  def isImage = imgFmtOpt.nonEmpty

  lazy val svgDocOpt: Option[Document] = {
    SvgUtil.safeOpenWrap(
      SvgUtil.open(file)
    )
  }

  lazy val svgGvtOpt: Option[GraphicsNode] = {
    for {
      svgDoc <- svgDocOpt
    } yield {
      SvgUtil.buildGvt(svgDoc)
    }
  }

  /** Ширина и длина картинки, когда это картинка. */
  lazy val imageWh: Option[MSize2di] = {
    for {
      imgFmt    <- imgFmtOpt
      mimeType  <- detectedMimeTypeOpt
    } yield {
      val res = imgFmt match {
        case MImgFmts.PNG | MImgFmts.JPEG | MImgFmts.GIF =>
          imgFileUtil.getImageWh( mimeType, file )
        case MImgFmts.SVG =>
          SvgUtil
            .getDocWh(svgDocOpt.get)
            .getOrElse {
              LOGGER.warn(s"$logPrefix No document wh. Drawing SVG to detect factical w/h. Usually differs from real document viewport sz.")
              svgGvtOpt
                .get
                .getPrimitiveBounds
                .toSize2di
            }
      }
      LOGGER.trace(s"$logPrefix sz=$res for imgFmt=$imgFmt mime=$mimeType")
      res
    }
  }

  /** Ранняя ''легковесная'' поверхностная валидация содержимого и размера файла. */
  lazy val validateFileContentEarly: Boolean = {
    imgFmtOpt
      .map { imgFmt =>
        try {
          // Это картинка. Проверить лимиты по размеру файла и размеру сторон картинки.
          (fileLength <= imgFmt.uploadMaxFileSizeB) &&
            imageWh.exists { wh =>
              val szMax = imgFmt.uploadSideSizeMaxPx
              (wh.width <= szMax) && (wh.height <= szMax)
            }
        } catch {
          case ex: Throwable =>
            LOGGER.error(s"validateFileContentEarly: Failed to validate file $file ${detectedMimeTypeOpt.orNull} ${fileLength}b", ex)
            false
        }
      }
      .getOrElse {
        // Неизвестный тип файла. Непонятно, как проверять.
        throw new UnsupportedOperationException(s"Don't know, how to early-validate ${detectedMimeTypeOpt.orNull}")
      }
  }


  lazy val identifyInfoOpt: Option[Future[Info]] = {
    for (mimg <- mLocalImgOpt) yield {
      mLocalImgs.identifyCached( mimg )
    }
  }

  /** Результат выполнения color-detect'ора над картинкой. */
  lazy val colorDetectOptFut: Option[Future[MHistogram]] = {
    for {
      mimg        <- mLocalImgOpt
      cdArgs      <- uploadArgs.info.colorDetect
    } yield {
      mainColorDetector.cached(mimg) {
        mainColorDetector.detectPaletteFor(mimg, maxColors = cdArgs.paletteSize)
      }
    }
  }


  /** Декодирование изображения в BufferedImage. */
  lazy val bufferedImageFutOpt: Future[Option[BufferedImage]] = {
    val optFut = for {
      mime    <- detectedMimeTypeOpt
      imgFmt  <- imgFmtOpt
    } yield {
      if (imgFmt !=* MImgFmts.SVG) {
        Future {
          imgFileUtil.readImage(mime, file)
        }
      } else {
        throw new UnsupportedOperationException("Image format not supported for ImageIO: " + imgFmt)
      }
    }
    FutureUtil.optFut2futOpt(optFut)(identity)
  }

  /** Процедура валидации самого изображения, т.е. полная проверка формата внутренностей файла. */
  lazy val validateImageOptFut: Option[Future[Boolean]] = {
    imgFmtOpt.map {
      case MImgFmts.PNG | MImgFmts.JPEG | MImgFmts.GIF =>
        bufferedImageFutOpt
          .map(_.nonEmpty)
          .recover { case ex: Throwable =>
            val logMgs = s"validateImageFut: Image file invalid, MIME=${detectedMimeTypeOpt.orNull}, file=$file"
            if (ex.isInstanceOf[NoSuchElementException])
              LOGGER.warn(logMgs)
            else
              LOGGER.warn(logMgs, ex)

            false
          }

      case MImgFmts.SVG =>
        Future.successful( svgGvtOpt.nonEmpty )
    }
  }

  /** Проверка загруженного файла. Здесь вызываются проверки в зависимости от фактического типа файла. */
  lazy val validateFileFut: Future[Boolean] = {
    if (isImage) {
      validateImageOptFut.get
    } else {
      // Это не картинка. Хз, как файл проверять
      LOGGER.error( s"validateFileFut: Don't know, how to validate file ${detectedMimeTypeOpt.orNull}" )
      Future.successful(false)
    }
  }

  /** Если изображение, то есть ли прозрачный цвет? */
  lazy val imageHasTransparentColors: Option[Future[Boolean]] = {
    imgFmtOpt.map {
      // JPEG не прозрачен
      case MImgFmts.JPEG =>
        Future.successful(false)
      // PNG/GIF - попытаться проверить варианты.
      case MImgFmts.PNG | MImgFmts.GIF =>
        bufferedImageFutOpt
          .map { bufOpt =>
            val buf = bufOpt.get
            // TODO Тут чисто вероятность. Надо перебирать пиксели, чтобы знать точно. А это долго. Или делать это на клиенте.
            buf.getColorModel.hasAlpha
          }
          .recover { case ex =>
            LOGGER.error("imageHasTransparentColors: Failed to get colors", ex)
            false
          }
      // у SVG почти всегда прозрачный фон, считаем его таким по дефолту.
      case MImgFmts.SVG =>
        Future.successful(true)
    }
  }

}
