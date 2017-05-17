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

  private def _ITYPE = MItemTypes.GeoLocCaptureArea
  private def _PRED  = MPredicates.NodeLocation.Paid


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

    di.advBuilderUtil.geoInstallNode(
      b0        = this2,
      items     = gItems,
      predicate = _PRED
    )
  }


  // TODO Надо брать только самую последнюю точку размещения и с ней плясать в installNode().
  // Предыдущие размещения закрывать с частичным возвратом средств.
  // А не накатывать всем покупками такого типа. См.TODO по installSql() ниже.
  // TODO Запилить installSql() или что-то такое, который откатывает все прошлые размещения, возвращает с них неизрасходованное бабло назад.

}
