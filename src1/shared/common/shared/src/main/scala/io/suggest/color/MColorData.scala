package io.suggest.color

import io.suggest.common.html.HtmlConstants
import io.suggest.err.ErrorConstants
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.math.MathConst
import io.suggest.scalaz.ScalazUtil
import io.suggest.text.StringUtil
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.17 15:16
  * Description: Пошаренная модель данных по одному цвету.
  */

object MColorData
  extends IEsMappingProps
{

  object Fields {
    /** Название поля с hex-кодом цвета. */
    val CODE_FN       = "c"
    val RGB_FN        = "rgb"
    val FREQ_PC_FN    = "q"
    val COUNT_FN      = "o"
  }

  object Examples {
    def WHITE = apply("FFFFFF")
    def BLACK = apply("000000")
  }

  /** Поддержка JSON. */
  implicit val MCOLOR_DATA_FORMAT: OFormat[MColorData] = {
    val F = Fields
    (
      (__ \ F.CODE_FN).format[String] and
      (__ \ F.RGB_FN).formatNullable[MRgb] and
      (__ \ F.FREQ_PC_FN).formatNullable[Int] and
      (__ \ F.COUNT_FN).formatNullable[Long]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MColorData] = UnivEq.derive


  def stripDiez(colorCode: String): String = {
    if (colorCode.startsWith( HtmlConstants.DIEZ )) {
      colorCode.replaceFirst( HtmlConstants.DIEZ, "" )
    } else {
      colorCode
    }
  }


  private def _vErrMsgF = ErrorConstants.emsgF("mcd")

  private def _validateHexCode(hexCode: String): ValidationNel[String, String] = {
    Validation.liftNel(hexCode.toLowerCase)({ h => !h.matches("[a-z0-9]{6}") }, _vErrMsgF("code") )
  }

  /** Полная валидация всех полей инстанса. */
  def validateFull(mcd: MColorData): ValidationNel[String, MColorData] = {
    (
      _validateHexCode(mcd.code) |@|
      ScalazUtil.liftNelOpt(mcd.rgb)( MRgb.validate ) |@|
      ScalazUtil.liftNelOpt(mcd.freqPc)( MathConst.Percents.validate_0_100(_, "fq") ) |@|
      ScalazUtil.liftNelOpt(mcd.count)( MathConst.Counts.validateMinMax(_, 1L, Int.MaxValue.toLong, "cnt") )
    ) { MColorData.apply }
  }

  /** Валидация только hex-кода с очисткой всех остальных полей. */
  def validateHexCodeOnly(mcd: MColorData): ValidationNel[String, MColorData] = {
    _validateHexCode(mcd.code)
      .map { MColorData(_) }
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.CODE_FN     -> FKeyWord.indexedJs,
      F.RGB_FN      -> FObject.nested( properties = MRgb.esMappingProps ),
      F.FREQ_PC_FN -> FNumber(
        typ = DocFieldTypes.Integer,
        index = someTrue,
      ),
      F.COUNT_FN -> FNumber(
        typ   = DocFieldTypes.Long,
        index = someFalse,
      )
    )
  }

  def code    = GenLens[MColorData](_.code)
  def rgb     = GenLens[MColorData](_.rgb)
  def freqPc  = GenLens[MColorData](_.freqPc)
  def count   = GenLens[MColorData](_.count)

}


/** Класс модели данных по одному цвету.
  *
  * @param code hex-код цвета БЕЗ # в начале.
  * @param rgb Индексируемый RGB-триплет цвета, если надо.
  * @param freqPc Нормированная частота в картинке в процентах, если есть.
  *               Т.е. Option[0..100%].
  *               Частота == 0 -- это возможно, если частота слишком низка.
  * @param count Абсолютное кол-во этого цвета.
  *              Например, кол-во пикселей такого цвета на изображении.
  */
case class MColorData(
                       code   : String,
                       rgb    : Option[MRgb]    = None,
                       freqPc : Option[Int]     = None,
                       count  : Option[Long]    = None
                     ) {

  // Тут для самоконтроля на ранних этапах использования. TODO Удалить этот мусор в будущем или вынести куда-нибудь:
  if (code startsWith HtmlConstants.DIEZ)
    throw new IllegalArgumentException("MColorData.code must NOT begin from #")

  if (freqPc.exists { freqPc => freqPc < 0 || freqPc > 100 } )
    throw new IllegalArgumentException("MColorData.freqPc must be None or between [0..100]")


  def hexCode = HtmlConstants.DIEZ + code

  private val _parsedRgb = MRgb.hex2rgb( code )

  /** Вернуть инстанс MRgb, даже если он отсутствует в полях. */
  def getRgb: MRgb = rgb.getOrElse( _parsedRgb )

  override def toString: String = {
    StringUtil.toStringHelper(this, 16) { renderF =>
      renderF("")(code)
      val F = MColorData.Fields
      rgb foreach renderF( F.RGB_FN )
      freqPc foreach renderF( F.FREQ_PC_FN )
      count foreach renderF( F.COUNT_FN )
    }
  }

}
