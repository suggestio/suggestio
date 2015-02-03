package models.im

import io.suggest.model.EnumValue2Val

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 17:46
 * Description:
 */


/** Выходные форматы картинок. */
object OutImgFmts extends Enumeration with EnumValue2Val {

  protected class Val(val name: String, val mime: String) extends super.Val(name)

  type OutImgFmt = Val
  override type T = OutImgFmt

  val JPEG: OutImgFmt = new Val("jpeg", "image/jpeg")
  val PNG: OutImgFmt  = new Val("png", "image/png")
  val GIF: OutImgFmt  = new Val("gif", "image/gif")
  val SVG: OutImgFmt  = new Val("svg", "image/svg+xml")

  /**
   * Предложить формат для mime-типа.
   * @param mime Строка mime-типа.
   * @return OutImgFmt. Если не-image тип, то будет IllegalArgumentException.
   */
  def forImageMime(mime: String): Option[OutImgFmt] = {
    values
      .find(_.mime equalsIgnoreCase mime)
      .asInstanceOf[Option[OutImgFmt]]
  }

}

