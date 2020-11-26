package util.up.ctx

import java.awt.image.BufferedImage
import java.nio.file.Path

import com.google.inject.assistedinject.Assisted
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.img.MImgFormats
import io.suggest.svg.SvgUtil
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import javax.inject.Inject
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
  def make(path: Path): ImgUploadCtx
}


/** Реализация upload-контекста на базе lazy vals. */
final class ImgUploadCtx @Inject()(
                                    @Assisted override val path             : Path,
                                    injector                                : Injector,
                                  )
  extends IUploadCtx
  with MacroLogsImplLazy
{

  override val file = super.file

  private lazy val imgFileUtil = injector.instanceOf[ImgFileUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private lazy val logPrefix = s"${getClass.getSimpleName}#${System.currentTimeMillis()}:"

  private lazy val imgFmtOpt = detectedMimeTypeOpt.flatMap( MImgFormats.withMime )


  private lazy val svgDocOpt: Option[Document] = {
    SvgUtil.safeOpenWrap(
      SvgUtil.open( file ),
      file.toString,
    )
  }

  private lazy val svgGvtOpt: Option[GraphicsNode] = {
    for {
      svgDoc <- svgDocOpt
    } yield {
      SvgUtil.buildGvt( svgDoc )
    }
  }

  def isImgSzValid(sz: MSize2di): Boolean =
    sz.height > 0 && sz.width > 0

  /** Ширина и длина картинки, когда это картинка. */
  override lazy val imageWh: Option[MSize2di] = {
    for {
      imgFmt    <- imgFmtOpt
      imgSz2d   <- imgFmt match {
        case MImgFormats.PNG | MImgFormats.JPEG | MImgFormats.GIF =>
          for {
            mimeType <- detectedMimeTypeOpt
            sz2d = imgFileUtil.getImageWh( mimeType, file )
            if {
              val r = isImgSzValid( sz2d )
              if (!r) LOGGER.warn(s"$logPrefix Invalid $imgFmt sz detected: $sz2d" )
              r
            }
          } yield sz2d

        case MImgFormats.SVG =>
          SvgUtil
            .getDocWh(svgDocOpt.get)
            .filter { res =>
              val r = isImgSzValid( res )
              if (!r) LOGGER.warn(s"$logPrefix Dropped invalid $imgFmt size detected by SvgUtil doc: $res")
              r
            }
            .orElse {
              for {
                svgGvt <- svgGvtOpt
                rect = svgGvt.getPrimitiveBounds
                sz2d = SvgUtil.rect2Size2d(
                  width0 = rect.getWidth,
                  height0 = rect.getHeight,
                )
                if {
                  val r = isImgSzValid(sz2d)
                  if (!r) LOGGER.warn(s"$logPrefix Invalid $imgFmt size from GVT: primitiveBounds=$sz2d (${svgGvt.getPrimitiveBounds}) || bounds=${svgGvt.getBounds} geometryBounds=${svgGvt.getGeometryBounds} sensitiveBounds=${svgGvt.getSensitiveBounds}")
                  r
                }
              } yield {
                LOGGER.debug(s"$logPrefix Draw SVG to detect factical w/h => $sz2d")
                sz2d
              }
            }
      }
    } yield {
      // Теоретически, можно организовать fallback через MLocalImg.getImageWh() для обеих ветвей. Это требует imageWh завернуть в Future[].
      LOGGER.trace(s"$logPrefix sz=$imgSz2d for imgFmt=$imgFmt")
      imgSz2d
    }
  }

  /** Ранняя ''легковесная'' поверхностная валидация содержимого и размера файла. */
  override def validateFileContentEarly(): Boolean = {
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
        LOGGER.info( s"Don't know, how to early-validate ${detectedMimeTypeOpt.orNull}" )
        true
      }
  }


  /** Декодирование изображения в BufferedImage. */
  private lazy val bufferedImageFutOpt: Future[Option[BufferedImage]] = {
    val optFut = for {
      mime    <- detectedMimeTypeOpt
      imgFmt  <- imgFmtOpt
    } yield {
      if (imgFmt !=* MImgFormats.SVG) {
        Future {
          imgFileUtil.readImage(mime, file)
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
      case MImgFormats.PNG | MImgFormats.JPEG | MImgFormats.GIF =>
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

      case MImgFormats.SVG =>
        Future.successful( svgGvtOpt.nonEmpty )
    }
      .get
  }

  /** Если изображение, то есть ли прозрачный цвет? */
  override def imageHasTransparentColors(): Option[Future[Boolean]] = {
    imgFmtOpt.map {
      // JPEG не прозрачен
      case MImgFormats.JPEG =>
        Future.successful(false)
      // PNG/GIF - попытаться проверить варианты.
      case MImgFormats.PNG | MImgFormats.GIF =>
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
      case MImgFormats.SVG =>
        Future.successful(true)
    }
  }

}
