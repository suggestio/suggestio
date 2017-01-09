package models.adv.geo.cur

import io.suggest.adv.geo.AdvGeoConstants.GjFtPropsC._
import io.suggest.mbill2.m.gid.Gid_t
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.16 17:31
  * Description: JSON-модель пропертей внутри GeoJSON Feature с данными для рендера.
  */

object GjFtProps {

  /** Поддержка JSON. По факту системе нужен только Writes. */
  implicit val FORMAT: OFormat[GjFtProps] = (
    (__ \ ITEM_ID_FN).format[Gid_t] and
    (__ \ HAS_APPROVED_FN).format[Boolean] and
    (__ \ CIRCLE_RADIUS_M_FN).formatNullable[Double]
  )(apply, unlift(unapply))

}


/** Класс модели пропертисов внутри GeoJSON Feature, описывающих элемент для рендера
  * в рамках формы-карты размещения на карте.
  *
  * @param itemId id по модели mitem, элемент которого содержит текущий шейп.
  *               При запросе попапа с сервера будет запрос всех остальных размещений исходя из этого id в качестве
  *               общепонятного id шейпа.
  * @param hasApproved Есть ли подтверждённые размещения, связанные с текущим шейпом?
  * */
case class GjFtProps(
  itemId          : Gid_t,
  hasApproved     : Boolean,
  crclRadiusM     : Option[Double]
)
