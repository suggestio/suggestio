package util.img

import java.io.File
import javax.inject.Inject

import io.suggest.svg.SvgUtil
import models.mfs.FileUtil
import net.sf.jmimemagic.MagicMatch

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

}
