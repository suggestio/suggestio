package util.img

import java.io.File
import javax.inject.Inject

import io.suggest.common.geom.d2.MSize2di
import io.suggest.svg.SvgUtil
import net.sf.jmimemagic.MagicMatch
import org.im4java.core.Info
import util.up.FileUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 11:16
 * Description: Утиль для работы с файлами изображений.
 */
class ImgFileUtil @Inject()(fileUtil: FileUtil) {

  def getMime(file: File): Option[String] = {
    getMime( fileUtil.getMimeMatch(file) )
  }
  def getMime(mmOpt: Option[MagicMatch]): Option[String] = {
    mmOpt.flatMap( getMime )
  }
  def getMime(mm: MagicMatch): Option[String] = {
    for {
      mime0 <- Option(mm.getMimeType)
    } yield {
      mime0 match {
        // jmimemagic хромает при определении mime-типа SVG.
        case textCt if SvgUtil.maybeSvgMime(textCt) =>
          "image/svg+xml"
        case other =>
          other
      }
    }
  }

  def orUnknown(mimeOpt: Option[String]): String = {
    mimeOpt getOrElse "image/unknown"
  }

  def getMimeOrUnknown(file: File): String = {
    orUnknown( getMime(file) )
  }


  /** Извлечь параметры картинки из identify Info. */
  def identityInfo2wh(info: Info): MSize2di = {
    MSize2di(
      height = info.getImageHeight,
      width  = info.getImageWidth
    )
  }

}
