package models.msc

import models.im.IImgWithWhInfo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:21
 * Description: Аргументы для рендера sc/welcomeTpl.
 */
trait WelcomeRenderArgsT {

  /** Фон. Либо Left(цвет), либо Right(инфа по картинке). */
  def bg: Either[String, IImgWithWhInfo]

  def fgImage: Option[IImgWithWhInfo]

  /** Текст, который надо отобразить. Изначально использовался, когда нет fgImage. */
  def fgText: Option[String]

  override def toString: String = {
    val sb = new StringBuilder(64, "bg=")
    bg match {
      case Right(ii)    => sb.append(ii.mimg.fileName)
      case Left(color)  => sb.append(color)
    }
    sb.append('&')
    val _fgi = fgImage
    if (_fgi.isDefined)
      sb.append("fgImage='")
        .append(_fgi.get.mimg.fileName)
        .append('\'')
        .append('&')
    val _fgt = fgText
    if (_fgt.isDefined)
      sb.append("fgText='")
        .append(_fgt.get)
        .append('\'')
    sb.toString()
  }
}


case class MWelcomeRenderArgs(
  override val bg       : Either[String, IImgWithWhInfo],
  override val fgImage  : Option[IImgWithWhInfo],
  override val fgText   : Option[String]
)
  extends WelcomeRenderArgsT
