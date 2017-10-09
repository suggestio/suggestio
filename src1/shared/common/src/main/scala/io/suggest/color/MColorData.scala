package io.suggest.color

import boopickle.Default._
import io.suggest.common.html.HtmlConstants
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.17 15:16
  * Description: Пошаренная модель данных по одному цвету.
  */

object MColorData {

  object Fields {
    /** Название поля с hex-кодом цвета. */
    val CODE_FN       = "c"
    val RGB_FN        = "rgb"
    val FREQ_PC_FN    = "fq"
  }

  /** Поддержка boopickle. */
  implicit val mColorDataPickler: Pickler[MColorData] = {
    generatePickler[MColorData]
  }

  /** Поддержка JSON. */
  implicit val MCOLOR_DATA_FORMAT: OFormat[MColorData] = {
    val F = Fields
    (
      (__ \ F.CODE_FN).format[String] and
      (__ \ F.RGB_FN).formatNullable[MRgb] and
      (__ \ F.FREQ_PC_FN).formatNullable[Int]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MColorData] = UnivEq.derive


  def stripDiez(colorCode: String): String = {
    if (colorCode.startsWith( HtmlConstants.DIEZ )) {
      colorCode.replaceFirst( HtmlConstants.DIEZ, "" )
    } else {
      colorCode
    }
  }

}


/** Класс модели данных по одному цвету.
  *
  * @param code hex-код цвета БЕЗ # в начале.
  * @param rgb Индексируемый RGB-триплет цвета, если надо.
  * @param freqPc Нормированная частота в картинке в процентах, если есть.
  *               Т.е. Option[0..100%].
  *               Частота == 0 -- это возможно, если частота слишком низка.
  */
case class MColorData(
                       code   : String,
                       rgb    : Option[MRgb]    = None,
                       freqPc : Option[Int]     = None
                     ) {

  // Тут для самоконтроля на ранних этапах использования. TODO Удалить этот мусор в будущем или вынести куда-нибудь:
  if (code startsWith HtmlConstants.DIEZ)
    throw new IllegalArgumentException("MColorData.code must NOT begin from #")

  if (freqPc.exists { freqPc => freqPc < 0 || freqPc > 100 } )
    throw new IllegalArgumentException("MColorData.freqPc must be None or between [0..100]")


  def hexCode = HtmlConstants.DIEZ + code

  def withCode(code: String)            = copy(code = code)
  def withRgb(rgb: Option[MRgb])        = copy(rgb = rgb)
  def withFreqPc(freqPc: Option[Int])   = copy(freqPc = freqPc)

}
