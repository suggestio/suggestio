package io.suggest.img.crop

import io.suggest.common.geom.d2.ISize2di
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 17:03
  * Description: Модель описания кропа картинки.
  */

object MCrop {

  private def optSign(v: Int, posSign: Char, acc: StringBuilder) {
    if (v < 0) {
      acc.append(v)
    } else {
      acc.append(posSign).append(v)
    }
  }

  /** Поддержка play-json. */
  implicit val MCROP_FORMAT: OFormat[MCrop] = (
    (__ \ "w").format[Int] and
    (__ \ "h").format[Int] and
    (__ \ "x").format[Int] and
    (__ \ "y").format[Int]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MCrop] = UnivEq.derive

}


case class MCrop(
                  width   : Int,
                  height  : Int,
                  offX    : Int,
                  offY    : Int
                )
  extends ISize2di {

  // TODO Исторический код сериализации в строку пока живёт здесь, хотя ему место где-то в районе ImgCropParsers, т.е. рядом с десериализатором.

  /**
   * Сконвертить в строку cropStr.
   * @return строку, пригодную для возврата в шаблоны/формы
   */
  def toCropStr: String = toStr('+')

  /**
   * Сериализовать данные в строку вида "WxH_offX-offY".
   *@return Строка, пригодная для использования в URL или ещё где-либо и обратимая назад в экземпляр ImgCrop.
   */
  def toUrlSafeStr: String = toStr('_')

  /** Хелпер для сериализации экземпляра класса. */
  def toStr(posSign: Char, szArgSuf: Option[Char] = None): String = {
    val sb = new StringBuilder(32)
    sb.append(width)
    szArgSuf foreach sb.append
    sb.append('x').append(height)
    szArgSuf foreach sb.append
    MCrop.optSign(offX, posSign, sb)
    MCrop.optSign(offY, posSign, sb)
    sb.toString()
  }

  /**
   * IM допускает задание размеров по отношению к текущим размерам изображения.
   * @return Строка вида 50%x60%+40+0
   */
  def toRelSzCropStr: String = toStr('+', Some('%'))

  override def toString = toCropStr

}
