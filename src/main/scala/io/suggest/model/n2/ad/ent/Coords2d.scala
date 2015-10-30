package io.suggest.model.n2.ad.ent

import java.{util => ju}

import io.suggest.common.geom.coord.ICoords2di
import io.suggest.model.es.EsModelUtil
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

  def getAndDeserializeCoord(fn: String, jm: ju.Map[_,_]): Int = {
    Option(jm.get(fn)).fold(0)(EsModelUtil.intParser)
  }

  // TODO Старая десериализация должна быть удалена, когда будет больше не нужна.
  val deserialize: PartialFunction[Any, Coords2d] = {
    case jm: ju.Map[_,_] =>
      Coords2d(
        x = getAndDeserializeCoord(X_FN, jm),
        y = getAndDeserializeCoord(Y_FN, jm)
      )
  }

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
{

  import Coords2d._

  // TODO Удалить после переключения на N2 с play.json.
  def renderPlayJsonFields(): JsObject = {
    JsObject(Seq(
      X_FN -> JsNumber(x),
      Y_FN -> JsNumber(y)
    ))
  }

}

