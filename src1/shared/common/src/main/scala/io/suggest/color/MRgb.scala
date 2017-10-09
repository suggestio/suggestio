package io.suggest.color

import java.awt.Color

import io.suggest.common.geom.coord.MCoords3d
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 16:51
  * Description: Модель RGB-цвета в виде int-триплета.
  */

/** Цвет-точка в 3-мерном пространстве цветов RGB. */
case class MRgb(red: Int, green: Int, blue: Int) {

  def toCoord3d = MCoords3d(x = red, y = green, z = blue)

}


object MRgb {

  object Fields {
    val RED_FN    = "r"
    val GREEN_FN  = "g"
    val BLUE_FN   = "b"
  }

  /** Поддержка play-json. */
  implicit val MRGB_FORMAT: OFormat[MRgb] = {
    val F = Fields
    (
      (__ \ F.RED_FN).format[Int] and
      (__ \ F.GREEN_FN).format[Int] and
      (__ \ F.BLUE_FN).format[Int]
    )(apply, unlift(unapply))
  }


  /**
    * Парсер из hex в [[MRgb]].
    * TODO И пока что он зависим от JVM...
    *
    * @param colorStr hex-строка вида "FFAA33" или "#FFAA33".
    * @return Инстанс RGB.
    *         Exception, если не удалось строку осилить.
    */
  def hex2rgb(colorStr: String): MRgb = {
    // TODO Задейстсовать MColorData().hexCode?
    val cs1 = if (colorStr startsWith "#")
      colorStr
    else
      "#" + colorStr
    // TODO Использовать Integer.parseInt("4F", 16)
    val c = Color.decode(cs1)
    MRgb(c.getRed, c.getGreen, c.getBlue)
  }

  implicit def univEq: UnivEq[MRgb] = UnivEq.derive

}

