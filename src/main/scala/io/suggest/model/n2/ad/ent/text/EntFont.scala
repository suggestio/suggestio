package io.suggest.model.n2.ad.ent.text

import java.{util => ju}

import io.suggest.model.es.EsModelUtil
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Entity font -- модель данных о шрифта для текстовых entities. */

object EntFont {

  val FAMILY_FN       = "family"
  val SIZE_FN         = "size"
  val COLOR_FN        = "color"
  val ALIGN_FN        = "align"

  def FONT_COLOR_DFLT = "FFFFFF"

  // TODO Выпилить, когда свершиться переезд на N2-архитектуру.
  val deserialize: PartialFunction[Any, EntFont] = {
    case jm: ju.Map[_,_] =>
      EntFont(
        color  = Option(jm.get(COLOR_FN))
          .fold(FONT_COLOR_DFLT)(EsModelUtil.stringParser),
        size   = Option(jm.get(SIZE_FN))
          .map(EsModelUtil.intParser),
        align  = Option(jm.get(ALIGN_FN))
          .map(EsModelUtil.stringParser)
          .flatMap(TextAligns.maybeWithName),
        family = Option(jm.get(FAMILY_FN))
          .map(EsModelUtil.stringParser)
      )
  }

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[EntFont] = (
    (__ \ COLOR_FN).formatNullable[String]
      .inmap[String](
        _ getOrElse FONT_COLOR_DFLT,
        Some.apply
      ) and
    (__ \ SIZE_FN).formatNullable[Int] and
    (__ \ ALIGN_FN).formatNullable[TextAlign] and
    (__ \ FAMILY_FN).formatNullable[String]
  )(apply, unlift(unapply))

}


import EntFont._


/**
 * Описание шрифтоты.
 * @param color Цвет шрифта.
 * @param size Необязательный размер шрифта.
 */
case class EntFont(
  color       : String            = EntFont.FONT_COLOR_DFLT,
  size        : Option[Int]       = None,
  align       : Option[TextAlign] = None,
  family      : Option[String]    = None
) {
  def renderPlayJsonFields(): JsObject = {
    var fieldsAcc: FieldsJsonAcc = List(
      COLOR_FN -> JsString(color)
    )
    if (family.isDefined)
      fieldsAcc ::= FAMILY_FN -> JsString(family.get)
    if (align.isDefined)
      fieldsAcc ::= ALIGN_FN -> JsString(align.get.toString())
    if (size.isDefined)
      fieldsAcc ::= SIZE_FN -> JsNumber(size.get)
    JsObject(fieldsAcc)
  }
}
