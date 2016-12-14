package models.adv.geo.mapf

import io.suggest.adv.geo.AdvGeoConstants.AdnNodes.Popup._
import models.mdt.MDateStartEndStr
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 17:26
  * Description: Аргументы для рендера шаблона попапа по одному узлу на карте георазмещения карточек.
  *
  * Работа идёт поверх пачки моделей: на первом месте -- маппинг формы, который задаёт весь рендер формы.
  * У формы есть список групп узлов и списки узлов в рамках этих групп.
  * И есть вспомогательная карта данных по узлам.
  *
  * 2016.dec.5: v2 модель пришла сразу после v1 и подразумевает использование react.js + JSON,
  * т.к. на фоне общей многоэтажной сложности возникла ещё бОльшая сложность с поддержкой этого всего
  * на стороне самой формы: при выборе галочек надо повторно всё рендерить внутрь внешней формы
  * (вне попапа карты), но уже на клиенте и чуть иначе. Происходит дублирование кода,
  * дублирующиеся зоопарки view-model'ов и куча контроллерного кода.
  *
  * 2016.dec.13: v3 модель для интеграции с react.js. Только JSON, только client-side render, только хардкор.
  */


// v3 ----------------------------------------------------

/** JSON-модель для описания содержимого попапа ресивера. */
object MRcvrReactPopupJson {
  implicit val WRITES: OWrites[MRcvrReactPopupJson] = {
    (__ \ GROUPS_FN).write[Seq[MNodeAdvGroupArgs]]
      .contramap{ a: MRcvrReactPopupJson => a.groups }
  }
}
case class MRcvrReactPopupJson(
  groups: Seq[MNodeAdvGroupArgs]
)


/** Модель группы под-узлов для размещения на ресивере. */
object MNodeAdvGroupArgs {
  implicit val WRITES: OWrites[MNodeAdvGroupArgs] = (
    (__ \ GROUP_ID_FN).writeNullable[String] and
    (__ \ NAME_FN).writeNullable[String] and
    (__ \ NODES_FN).write[Seq[MNodeAdvInfo]]
  )(unlift(unapply))
}
/** Класс модели группы под-узлов для размещения на ресивере. */
case class MNodeAdvGroupArgs(
  groupId   : Option[String],
  nameOpt   : Option[String],
  nodes     : Seq[MNodeAdvInfo]
)


/** Модель JSON-описания одного узла в списке узлов для попапа размещения в ресивере. */
object MNodeAdvInfo {

  implicit val WRITES: OWrites[MNodeAdvInfo] = (
    (__ \ NODE_ID_FN).write[String] and
    (__ \ IS_CREATE_FN).write[Boolean] and
    (__ \ CHECKED_FN).write[Boolean] and
    (__ \ NAME_FN).writeNullable[String] and
    (__ \ IS_ONLINE_NOW_FN).write[Boolean] and
    (__ \ INTERVAL_FN).writeNullable[MDateStartEndStr]
  )( unlift(unapply) )

}
/** Класс JSON-модели описания одного узла в списке размещения. */
case class MNodeAdvInfo(
  nodeId          : String,
  isCreate        : Boolean,
  checked         : Boolean,
  nameOpt         : Option[String],
  isOnlineNow     : Boolean,
  intervalOpt     : Option[MDateStartEndStr]
)
