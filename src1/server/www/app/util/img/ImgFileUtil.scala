package util.img

import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import javax.inject.Inject

import io.suggest.common.geom.d2.MSize2di
import org.im4java.core.Info
import util.up.FileUtil

import scala.collection.JavaConverters._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:16
 * Description: Утиль для работы с файлами изображений.
 */
class ImgFileUtil @Inject()(
                             fileUtil: FileUtil
                           ) {

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

  /** Прочитать из файла размеры изображения.
    *
    * @param mimeType Тип файла.
    * @param imgFile Обрабатываемый файл с картинкой.
    * @return Инстанс MSize2di, когда всё ок.
    */
  def getImageWh(mimeType: String, imgFile: File): MSize2di = {
    ImageIO
      .getImageReadersByMIMEType(mimeType)
      .asScala
      .map { reader =>
        try {
          val is = new FileImageInputStream(imgFile)
          try {
            reader.setInput(is)
            MSize2di(
              width  = reader.getWidth( reader.getMinIndex ),
              height = reader.getHeight( reader.getMinIndex )
            )
          } finally {
            is.close()
          }

        } finally {
          reader.dispose()
        }
      }
      .next()
  }

}
