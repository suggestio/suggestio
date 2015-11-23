package io.suggest.sjs.common.geom

import io.suggest.common.geom.coord.{ICoords2di, ICoord2d}
import org.scalajs.dom.Touch

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.15 13:36
 * Description: Реализация координат для нужд
 */

object Coord2dD {

  /** Сборка double-коордианты на основе экземпляра js touch. */
  def apply(touch: Touch): Coord2dD = {
    apply(x = touch.pageX, y = touch.clientY)
  }

}

/** Двумерные double-координаты. */
case class Coord2dD(
  override val x: Double,
  override val y: Double
)
  extends ICoord2d[Double]


case class Coords2di(
  override val x: Int,
  override val y: Int
)
  extends ICoords2di
