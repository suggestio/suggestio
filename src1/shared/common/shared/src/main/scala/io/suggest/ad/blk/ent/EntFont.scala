package io.suggest.ad.blk.ent

import io.suggest.common.empty.EmptyUtil._
import io.suggest.font.{MFont, MFontSize}
import io.suggest.text.MTextAlign
import play.api.libs.functional.syntax._
import play.api.libs.json._

/** Entity font -- модель данных о шрифта для текстовых entities. */

object EntFont {

  // Нельзя переименовывать значения имён полей: это хранится в ES!
  val FAMILY_FN       = "family"
  val SIZE_FN         = "size"
  val COLOR_FN        = "color"
  val ALIGN_FN        = "align"

  def FONT_COLOR_DFLT = "FFFFFF"

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[EntFont] = (
    (__ \ COLOR_FN).formatNullable[String]
      .inmap[String](
        _.getOrElse(FONT_COLOR_DFLT),
        someF
      ) and
    (__ \ SIZE_FN).formatNullable[MFontSize] and
    (__ \ ALIGN_FN).formatNullable[MTextAlign] and
    (__ \ FAMILY_FN).formatNullable[MFont]
  )(apply, unlift(unapply))

}


/**
 * Описание шрифтоты.
 * @param color Цвет шрифта.
 * @param size Необязательный размер шрифта.
  */
case class EntFont(
                    color       : String              = EntFont.FONT_COLOR_DFLT,
                    size        : Option[MFontSize]   = None,
                    align       : Option[MTextAlign]  = None,
                    family      : Option[MFont]       = None
                  )
