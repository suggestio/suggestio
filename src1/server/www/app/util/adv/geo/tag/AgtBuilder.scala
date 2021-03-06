package util.adv.geo.tag

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.n2.edge._
import japgolly.univeq._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.16 15:15
  * Description: Трейт поддержки adv-билдинга для гео-тегов.
  */
trait AgtBuilder extends IAdvBuilder {

  private final def _PRED   = MPredicates.TaggedBy.AdvGeoTag
  private final def _ITYPE  = MItemTypes.GeoTag


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
