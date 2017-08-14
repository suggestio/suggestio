package io.suggest.model.n2.ad.ent.text

import io.suggest.common.empty.EmptyUtil._
import io.suggest.text.MTextAlign
import play.api.libs.functional.syntax._
import play.api.libs.json._

/** Entity font -- модель данных о шрифта для текстовых entities. */

object EntFont {

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
    (__ \ SIZE_FN).formatNullable[Int] and
    (__ \ ALIGN_FN).formatNullable[MTextAlign] and
    (__ \ FAMILY_FN).formatNullable[String]
  )(apply, unlift(unapply))

}



/**
 * Описание шрифтоты.
 * @param color Цвет шрифта.
 * @param size Необязательный размер шрифта.
  */
case class EntFont(
                    color       : String              = EntFont.FONT_COLOR_DFLT,
                    size        : Option[Int]         = None,
                    align       : Option[MTextAlign]  = None,
                    family      : Option[String]      = None
                  )
