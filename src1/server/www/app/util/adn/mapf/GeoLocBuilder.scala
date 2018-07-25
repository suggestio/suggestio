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
trait GeoLocBuilder extends IAdvBuilder {

  private final def _ITYPE = MItemTypes.GeoLocCaptureArea
  private final def _PRED  = MPredicates.NodeLocation.Paid


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
      val lastItemOpt = di.advBuilderUtil.lastStartedItem(gItems)
      LOGGER.debug(s"installNode(): There are ${gItems.size} items to geoInstallNode (##${gItems.iterator.flatMap(_.id).mkString(",")}), but we interested only in last item#${lastItemOpt.flatMap(_.id).orNull}.")
      di.advBuilderUtil.geoInstallNode(
        b0        = this2,
        // Интересует только самый последний item
        items     = lastItemOpt.toList,
        predicate = _PRED,
        // Индексируем имя, чтобы работал поиск узлов на карте.
        name2tag  = true
      )
    }
  }


  /** Аналогично [[AdnMapBuilder]], надо дропать предыдущие размещения. */
  override def installSql(items: Iterable[MItem]): IAdvBuilder = {
    val b0 = super.installSql(items)
    // Предварительно завершить все остальные adn-item'ы с частичным возвратом средств или без оного.
    di.advBuilderUtil.interruptAdnMapItemsFor(b0, items)
  }

}
