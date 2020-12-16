package util.adn.mapf

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.2020 16:00
  * Description: Поддержка location-тегов наподобии GeoTag'ов.
  */
trait GeoLocTagBuilder extends IAdvBuilder {

  private final def _ITYPE = MItemTypes.LocationTag
  private final def _PRED  = MPredicates.TaggedBy.LocationTag

  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  override def clearNodePredicates: List[MPredicate] = {
    _PRED :: super.clearNodePredicates
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    di.advBuilderUtil.installNodeGeoTag(this, items, _ITYPE, _PRED, super.installNode)
  }

  /** Теги требуют обработки с выставлением MItem.tagNodeId. */
  override def installSql(items: Iterable[MItem]): IAdvBuilder = {
    di.advBuilderUtil.tagsInstallSql(this, items, _ITYPE, super.installSql )
  }

}
