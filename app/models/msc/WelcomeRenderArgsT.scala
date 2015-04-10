package models.msc

import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:21
 * Description: Аргументы для рендера sc/welcomeTpl.
 */
trait WelcomeRenderArgsT {

  /** Фон. Либо Left(цвет), либо Right(инфа по картинке). */
  def bg: Either[String, ImgUrlInfoT]

  def fgImage: Option[MImgInfoT]

  /** Текст, который надо отобразить. Изначально использовался, когда нет fgImage. */
  def fgText: Option[String]

  override def toString: String = {
    val sb = new StringBuilder(64, "bg=")
    bg match {
      case Right(ii) => sb.append(ii.call.url)
      case Left(color) => sb.append(color)
    }
    sb.append('&')
    val _fgi = fgImage
    if (_fgi.isDefined)
      sb.append("fgImage='").append(_fgi.get.filename).append('\'').append('&')
    val _fgt = fgText
    if (_fgt.isDefined)
      sb.append("fgText='").append(_fgt.get).append('\'')
    sb.toString()
  }
}
