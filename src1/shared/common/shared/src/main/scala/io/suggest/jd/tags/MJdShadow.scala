package io.suggest.jd.tags

import io.suggest.color.{IColorPickerMarker, MColorData}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.18 18:09
  * Description: Модель данных тени.
  * Модель должна покрывать и text-shadow, и box-shadow и быть максимально гибкой,
  * поэтому обязательны только X,Y-параметры тени.
  */
object MJdShadow {

  /** Поддержка play-json. */
  implicit def jdShadowFormat: OFormat[MJdShadow] = (
    (__ \ "h").format[Int] and
    (__ \ "v").format[Int] and
    (__ \ "c").formatNullable[MColorData] and
    (__ \ "b").formatNullable[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MJdShadow] = UnivEq.derive

  object ColorMarkers {
    case object TextShadow extends IColorPickerMarker
  }

  def hOffset = GenLens[MJdShadow](_.hOffset)
  def vOffset = GenLens[MJdShadow](_.vOffset)
  def color   = GenLens[MJdShadow](_.color)
  def blur    = GenLens[MJdShadow](_.blur)

}


/** Контейнер данных тени.
  *
  * @param hOffset Сдвиг по горизонтали.
  * @param vOffset Сдвиг по вертикали.
  * @param color Цвет тени (1/10).
  * @param blur Параметр размывки.
  */
case class MJdShadow(
                      hOffset   : Int,
                      vOffset   : Int,
                      color     : Option[MColorData],
                      blur      : Option[Int],
                    )
