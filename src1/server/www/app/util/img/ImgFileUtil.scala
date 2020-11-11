package util.img

import java.awt.image.BufferedImage
import java.io.File

import javax.imageio.{ImageIO, ImageReader}
import javax.imageio.stream.FileImageInputStream
import io.suggest.common.geom.d2.MSize2di
import io.suggest.util.logs.MacroLogsImplLazy
import org.im4java.core.Info

import scala.concurrent.blocking
import scala.jdk.CollectionConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:16
 * Description: Утиль для работы с файлами изображений.
 */
final class ImgFileUtil extends MacroLogsImplLazy {

  def orUnknown(mimeOpt: Option[String]): String = {
    mimeOpt getOrElse "image/unknown"
  }

  /** Извлечь параметры картинки из identify Info. */
  def identityInfo2wh(info: Info): MSize2di = {
    MSize2di(
      height = info.getImageHeight,
      width  = info.getImageWidth
    )
  }


  def forMimeType[T](mimeType: String)(f: ImageReader => T): T = {
    ImageIO
      .getImageReadersByMIMEType(mimeType)
      .asScala
      .map( f )
      .next()
  }

  def forImageWithMime[T](mimeType: String, imgFile: File)(f: (ImageReader, FileImageInputStream) => T): T = {
    forMimeType(mimeType) { reader =>
      blocking {
        val stream = new FileImageInputStream(imgFile)
        try {
          reader.setInput(stream)
          f(reader, stream)
        } finally {
          stream.close()
        }
      }
    }
  }


  /** Попытаться прочитать всё изображение в память.
    * Пригодно для тестирования изображения силами только JVM, что довольно безопасно, хоть и может иметь
    * ложные срабатывания ошибок, по сравнению с imagemagick, в котором встречаются совсем не ложные уязвимости.
    *
    * @param mimeType MIME-тип изображения.
    * @param imgFile Файл с картинкой.
    * @return None при проблеме
    *         Some(awt image) когда всё ок.
    */
  def readImage(mimeType: String, imgFile: File): Option[BufferedImage] = {
    val startTimeMs = System.currentTimeMillis()
    lazy val logPrefix = s"readImage()#$startTimeMs"
    LOGGER.trace(s"$logPrefix MIME=$mimeType file=$imgFile")

    try {
      forImageWithMime(mimeType, imgFile) { (reader, _) =>
        val res = Option(
          reader.read(reader.getMinIndex)
        )
        LOGGER.debug(s"$logPrefix Finished, success?${res.nonEmpty}, took=${System.currentTimeMillis() - startTimeMs}ms")
        res
      }
    } catch {
      case ex: Throwable =>
        LOGGER.error(s"$logPrefix Failed to read $mimeType from $imgFile", ex)
        None
    }
  }


  /** Очень быстрое определение ширины и длины изображения на основе заголовка.
    *
    * @param mimeType Тип файла.
    * @param imgFile Обрабатываемый файл с картинкой.
    * @return Инстанс MSize2di, когда всё ок.
    */
  def getImageWh(mimeType: String, imgFile: File): MSize2di = {
    forImageWithMime(mimeType, imgFile) { (reader, _) =>
      MSize2di(
        width  = reader.getWidth( reader.getMinIndex ),
        height = reader.getHeight( reader.getMinIndex )
      )
    }
  }

}
