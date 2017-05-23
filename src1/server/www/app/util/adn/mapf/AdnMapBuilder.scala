package util.adn.mapf

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.11.16 14:34
  * Description: Billing adv-builder для размещения ADN-узла на карте.
  *
  * Изначальная задумка сильно аналогична AgpBuilder: размещение узла в геошейпе/геоточке на карте.
  * Максимум одна точка на один узел.
  *
  * Изначальная реализация: один узел может иметь много точек. Пока без отката предыдущих точек-размещений.
  */
trait AdnMapBuilder extends IAdvBuilder {

  private def _ITYPE = MItemTypes.AdnNodeMap
  private def _PRED  = MPredicates.AdnMap


  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  override def clearNodePredicates: List[MPredicate] = {
    _PRED :: super.clearNodePredicates
  }

  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    // Интересуют только item'ы определённого типа...
    val (gItems, other) = di.advBuilderUtil.partitionItemsByType(items, _ITYPE)

    val this2 = super.installNode(other)

    if (gItems.isEmpty) {
      this2
    } else {
      di.advBuilderUtil.geoInstallNode(
        b0        = this2,
        // Надо брать только самую свежую (последнюю) точку для размещения среди gItems.
        items     = di.advBuilderUtil.lastStartedItem(gItems).toList,
        predicate = _PRED
      )
    }
  }

  override def installSql(items: Iterable[MItem]): IAdvBuilder = {
    val b0 = super.installSql(items)
    // Предварительно завершить все остальные текущие item'ы текущего типа с частичным возвратом средств или без оного.
    di.advBuilderUtil.interruptItemsFor(b0, items, _ITYPE)
  }

}
