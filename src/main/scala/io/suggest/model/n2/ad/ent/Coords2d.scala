package io.suggest.model.n2.ad.ent

import io.suggest.common.geom.coord.ICoords2di
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 14:08
 * Description: Модель двумерных координат объектов/сущностей рекламной карточки.
 */

object Coords2d {

  val X_FN = "x"
  val Y_FN = "y"

  /** Поддержка JSON для экземпляров модели черех play-json. */
  implicit val FORMAT: OFormat[Coords2d] = (
    (__ \ X_FN).format[Int] and
    (__ \ Y_FN).format[Int]
  )(apply, unlift(unapply))
  
}



case class Coords2d(
  override val x: Int,
  override val y: Int
)
  extends ICoords2di

