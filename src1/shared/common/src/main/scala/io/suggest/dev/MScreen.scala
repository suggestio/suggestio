package io.suggest.dev

import io.suggest.common.geom.d2.ISize2di
import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, __}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */

object MScreen {

  /** Поддержка Play-json сериализации у нас очень простая. */
  implicit val MSCREEN_WRITES: Writes[MScreen] = {
    __.write[String]
      .contramap[MScreen]( _.toQsValue )
  }


  def roundPxRation(pxRatioRaw: Double): Double = {
    Math.round(pxRatioRaw * 10) / 10
  }

}


/** Класс модели для client-side описания экрана. */
case class MScreen(
  override val width    : Int,
  override val height   : Int,
  pxRatio               : Double
)
  extends ISize2di
{

  /** Сериализовать для передачи на сервер. */
  def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    width.toString + "x" + height.toString + "," + pxRatio
  }

  override def toString: String = toQsValue

}

