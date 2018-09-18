package io.suggest.img.crop

import io.suggest.common.geom.d2.{IHeight, ISize2di, IWidth, MSize2di}
import io.suggest.math.MathConst
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.ValidationNel
import scalaz.syntax.apply._

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

  object Fields {
    val WIDTH_FN    = "w"
    val HEIGHT_FN   = "h"
    val OFFSET_X_FN = "x"
    val OFFSET_Y_FN = "y"
  }

  /** Поддержка play-json. */
  implicit val MCROP_FORMAT: OFormat[MCrop] = {
    val F = Fields
    (
      (__ \ F.WIDTH_FN).format[Int] and
      (__ \ F.HEIGHT_FN).format[Int] and
      (__ \ F.OFFSET_X_FN).format[Int] and
      (__ \ F.OFFSET_Y_FN).format[Int]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MCrop] = UnivEq.derive


  /** Валидация в контексте картинки и контейнера.
    *
    * @param crop Валидируемый кроп.
    * @param tgContSz Размеры целевого контейнера, ради которых происходил кроп.
    * @param imgWh Размеры исходной картинки, которую кропали.
    * @return Результат валидации с обновлённым инстансом MCrop.
    */
  def validate(crop: MCrop, tgContSz: ISize2di, imgWh: ISize2di): ValidationNel[String, MCrop] = {
    val C = MathConst.Counts

    def _validateSide(sideF: ISize2di => Int, fn: String) = {
      C.validateMinMax(
        v   = sideF(crop),
        min = sideF(tgContSz),
        max = sideF(imgWh),
        eMsgPrefix = fn
      )
    }

    val offsetsAsWh = crop.offsets2side2d
    def _validateOffset(sideF: ISize2di => Int, fn: String) = {
      C.validateMinMax(
        v   = sideF(offsetsAsWh),
        min = -sideF(imgWh),
        max = sideF(imgWh) - sideF(crop),
        eMsgPrefix = "off" + fn.toUpperCase()
      )
    }

    val F = Fields
    (
      _validateSide(IWidth.f,     F.WIDTH_FN)     |@|
      _validateSide(IHeight.f,    F.HEIGHT_FN)    |@|
      _validateOffset(IWidth.f,   F.OFFSET_X_FN)  |@|
      _validateOffset(IHeight.f,  F.OFFSET_Y_FN)
    ) { MCrop.apply }
    // Пересборка инстанса просто для самоконтроля на случай добавления новых полей в конструктор класса.
  }

}


/** Модель описания кропа.
  *
  * @param width Ширина кропа.
  * @param height Высота кропа.
  * @param offX Сдвиг по горизонтали.
  * @param offY Сдвиг по вертикали.
  */
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

  /** Изобразить оффсеты как размеры. */
  def offsets2side2d = MSize2di(width = offX, height = offY)

  /**
   * IM допускает задание размеров по отношению к текущим размерам изображения.
   * @return Строка вида 50%x60%+40+0
   */
  def toRelSzCropStr: String = toStr('+', Some('%'))

  override def toString = toCropStr

}
