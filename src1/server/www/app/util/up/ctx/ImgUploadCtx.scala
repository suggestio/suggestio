package util.up.ctx

import java.awt.image.BufferedImage

import com.google.inject.assistedinject.Assisted
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.geom.d2.MSize2diJvm.Implicits._
import io.suggest.img.MImgFmts
import io.suggest.svg.SvgUtil
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import javax.inject.Inject
import models.mup.MUploadCtxArgs
import org.apache.batik.gvt.GraphicsNode
import org.w3c.dom.Document
import play.api.inject.Injector
import util.img.ImgFileUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.18 21:24
  * Description: Модели контекста аплоада.
  */


/** Интерфейс для Guice DI-factory, которая собирает инстансы upload-контекста [[ImgUploadCtx]]. */
trait IImgUploadCtxFactory {
  def make( upCtxArgs: MUploadCtxArgs ): ImgUploadCtx
}

/** Реализация upload-контекста на базе lazy vals. */
class ImgUploadCtx @Inject()(
                              @Assisted upCtxArgs          : MUploadCtxArgs,
                              injector                     : Injector,
                            )
  extends IUploadCtx
  with MacroLogsImplLazy
{

  private lazy val imgFileUtil = injector.instanceOf[ImgFileUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

  private lazy val imgFmtOpt = upCtxArgs.detectedMimeTypeOpt.flatMap( MImgFmts.withMime )


  private lazy val svgDocOpt: Option[Document] = {
    SvgUtil.safeOpenWrap(
      SvgUtil.open( upCtxArgs.file )
    )
  }

  private lazy val svgGvtOpt: Option[GraphicsNode] = {
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
      mimeType  <- upCtxArgs.detectedMimeTypeOpt
    } yield {
      val res = imgFmt match {
        case MImgFmts.PNG | MImgFmts.JPEG | MImgFmts.GIF =>
          imgFileUtil.getImageWh( mimeType, upCtxArgs.file )
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
  override def validateFileContentEarly(): Boolean = {
    imgFmtOpt
      .map { imgFmt =>
        try {
          // Это картинка. Проверить лимиты по размеру файла и размеру сторон картинки.
          (upCtxArgs.fileLength <= imgFmt.uploadMaxFileSizeB) &&
            imageWh.exists { wh =>
              val szMax = imgFmt.uploadSideSizeMaxPx
              (wh.width <= szMax) && (wh.height <= szMax)
            }
        } catch {
          case ex: Throwable =>
            LOGGER.error(s"validateFileContentEarly: Failed to validate file ${upCtxArgs.file} ${upCtxArgs.detectedMimeTypeOpt.orNull} ${upCtxArgs.fileLength}b", ex)
            false
        }
      }
      .getOrElse {
        // Неизвестный тип файла. Непонятно, как проверять.
        LOGGER.info( s"Don't know, how to early-validate ${upCtxArgs.detectedMimeTypeOpt.orNull}" )
        true
      }
  }


  /** Декодирование изображения в BufferedImage. */
  private lazy val bufferedImageFutOpt: Future[Option[BufferedImage]] = {
    val optFut = for {
      mime    <- upCtxArgs.detectedMimeTypeOpt
      imgFmt  <- imgFmtOpt
    } yield {
      if (imgFmt !=* MImgFmts.SVG) {
        Future {
          imgFileUtil.readImage(mime, upCtxArgs.file)
        }
      } else {
        throw new UnsupportedOperationException(s"$logPrefix Image format not supported for ImageIO: $imgFmt")
      }
    }
    FutureUtil.optFut2futOpt(optFut)(identity)
  }

  /** Проверка загруженного файла. Здесь вызываются проверки в зависимости от фактического типа файла. */
  override def validateFileFut(): Future[Boolean] = {
    imgFmtOpt.map {
      case MImgFmts.PNG | MImgFmts.JPEG | MImgFmts.GIF =>
        bufferedImageFutOpt
          .map(_.nonEmpty)
          .recover { case ex: Throwable =>
            val logMgs = s"validateImageFut: Image file invalid, MIME=${upCtxArgs.detectedMimeTypeOpt.orNull}, file=${upCtxArgs.file}"
            if (ex.isInstanceOf[NoSuchElementException])
              LOGGER.warn(logMgs)
            else
              LOGGER.warn(logMgs, ex)

            false
          }

      case MImgFmts.SVG =>
        Future.successful( svgGvtOpt.nonEmpty )
    }
      .get
  }

  /** Если изображение, то есть ли прозрачный цвет? */
  override def imageHasTransparentColors(): Option[Future[Boolean]] = {
    imgFmtOpt.map {
      // JPEG не прозрачен
      case MImgFmts.JPEG =>
        Future.successful(false)
      // PNG/GIF - попытаться проверить варианты.
      case MImgFmts.PNG | MImgFmts.GIF =>
        bufferedImageFutOpt
          .map { bufOpt =>
            // TODO Тут чисто вероятность. Надо перебирать пиксели, чтобы знать точно. А это долго. Или делать это на клиенте.
            bufOpt.exists(_.getColorModel.hasAlpha)
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