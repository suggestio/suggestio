package models.mwc

import models.im.MImgWithWhInfo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:21
 * Description: Аргументы для рендера sc/welcomeTpl.
 */
final case class MWelcomeRenderArgs(
                                     bg       : Either[String, MImgWithWhInfo],
                                     fgImage  : Option[MImgWithWhInfo],
                                     fgText   : Option[String]
                                   ) {

  override def toString: String = {
    val sb = new StringBuilder(64, "bg=")
    bg match {
      case Right(ii)    => sb.append(ii.mimg.dynImgId.fileName)
      case Left(color)  => sb.append(color)
    }
    sb.append('&')
    val _fgi = fgImage
    if (_fgi.isDefined)
      sb.append("fgImage='")
        .append(_fgi.get.mimg.dynImgId.fileName)
        .append('\'')
        .append('&')
    val _fgt = fgText
    if (_fgt.isDefined)
      sb.append("fgText='")
        .append(_fgt.get)
        .append('\'')
    sb.toString()
  }


  def allImgsWithWhInfoIter: Iterator[MImgWithWhInfo] = {
    (bg.toOption :: fgImage :: Nil)
      .iterator
      .flatten
  }

}
