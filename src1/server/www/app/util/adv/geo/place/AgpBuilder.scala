package util.adv.geo.place

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemTypes, MItemType}
import io.suggest.model.n2.edge._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 18:44
  * Description: Трейт поддержки размещения в гео-точке для Adv-билдера.
  */
trait AgpBuilder extends IAdvBuilder {

  private def _ITYPE = MItemTypes.GeoPlace
  private def _PRED  = MPredicates.AdvGeoPlace

  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  override def clearNodePredicates: List[MPredicate] = {
    _PRED :: super.clearNodePredicates
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    val (gItems, other) = di.advBuilderUtil.partitionItemsByType(items, _ITYPE)
    val this2 = super.installNode(other)

    if (gItems.nonEmpty)
      LOGGER.debug(s"installNode(): Found ${gItems.size} geo-place items for geoInstallNode processing: [${gItems.iterator.flatMap(_.id).mkString(",")}]")

    di.advBuilderUtil.geoInstallNode(
      b0        = this2,
      items     = gItems,
      predicate = _PRED,
      name2tag  = false
    )
  }

}
