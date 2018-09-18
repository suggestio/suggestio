package io.suggest.color

import io.suggest.common.geom.coord.MCoords3d
import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 16:51
  * Description: Модель RGB-цвета в виде int-триплета.
  * Поле alpha добавлено позже.
  */

/** Цвет-точка в 3-мерном пространстве цветов RGB. */
case class MRgb(red: Int, green: Int, blue: Int, alpha: Option[Double] = None) {

  def toCoord3d = MCoords3d(x = red, y = green, z = blue)

}


object MRgb {

  object Fields {
    val RED_FN    = "r"
    val GREEN_FN  = "g"
    val BLUE_FN   = "b"
    val ALPHA_FN  = "a"
  }

  /** Поддержка play-json. */
  implicit val MRGB_FORMAT: OFormat[MRgb] = {
    val F = Fields
    (
      (__ \ F.RED_FN).format[Int] and
      (__ \ F.GREEN_FN).format[Int] and
      (__ \ F.BLUE_FN).format[Int] and
      (__ \ F.ALPHA_FN).formatNullable[Double]
    )(apply, unlift(unapply))
  }


  /**
    * Парсер из hex в [[MRgb]].
    * Пока без поддержки RGBA-формата.
    *
    * @param colorStr hex-строка вида "FFAA33" или "#FFAA33".
    * @return Инстанс RGB.
    *         Exception, если не удалось строку осилить.
    */
  def hex2rgb(colorStr: String): MRgb = {
    // Если 8+ символов, то значит есть alpha-канал внутри hex-значения.
    val EIGHT = 8
    val ZERO = 0
    val SIX = 6
    val SIXTEEN = EIGHT * 2
    //val isWithAlpha = colorStr.length >= EIGHT
    // Если есть alpha-канал, то нужно дополнительно сдвигать все вычисления на байт.
    //val rgbAlphaOffset = if (isWithAlpha) EIGHT else ZERO
    // Убедиться, что # в начале нет, чтобы Integer.parseInt() не сломался.
    var colorStrNoDiez =
      if (colorStr startsWith HtmlConstants.DIEZ) colorStr.tail
      else colorStr
    // Укоротить до 6 символов, если требуется. Обычно это не нужно.
    if (colorStrNoDiez.length > SIX)
      colorStrNoDiez = colorStrNoDiez.substring(ZERO, SIX)
    val intval = Integer.parseInt(colorStrNoDiez, SIXTEEN)
    val i = intval.intValue()
    def __extractShortInt(bitOffset: Int): Int = {
      //val realBitOffset = rgbAlphaOffset + bitOffset
      val i2 = if (bitOffset > ZERO) i >> bitOffset else i
      i2 & 0xFF
    }
    MRgb(
      red   = __extractShortInt(SIXTEEN),     // (i >> 16) & 0xFF,
      green = __extractShortInt(EIGHT),       // (i >> 8) & 0xFF,
      blue  = __extractShortInt(ZERO)         // i & 0xFF
      // TODO Альфа-канал. parseInt() не может распарсить 4 байта, только 3, т.к. в java/scala int только signed. Возможно, окостылить parseUnsigned?
      /*alpha = OptionUtil.maybe(isWithAlpha)(
        __extractShortInt(-EIGHT).toDouble / 255d
      )*/
    )
  }


  @inline implicit def univEq: UnivEq[MRgb] = UnivEq.derive


  /** Валидация значений через scalaz. */
  def validate(mrgb: MRgb): ValidationNel[String, MRgb] = {
    def isColorValid(color: Int): Boolean = {
      color >= 0 && color <= 255
    }
    val rgbErr = ErrorConstants.emsgF("rgb")
    (
      Validation.liftNel(mrgb.red)(isColorValid,    rgbErr("red")) |@|
      Validation.liftNel(mrgb.green)(isColorValid,  rgbErr("green")) |@|
      Validation.liftNel(mrgb.blue)(isColorValid,   rgbErr("blue")) |@|
      Validation.liftNel(mrgb.alpha)(
        _.exists(a => a > 1.0 || a < 0d),
        rgbErr("alpha")
      )
    )( MRgb.apply )
    // Не ясно, нужна ли пересборка инстанса. С очевидной стороны -- не нужна,
    // а с другой: если вдруг появится новое поле в классе, но забыть дописать
    // валидатор для нового поля, то тут сразу будет ошибка компиляции.
  }

}

