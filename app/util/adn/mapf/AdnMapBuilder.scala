package util.adn.mapf

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import io.suggest.ym.model.NodeGeoLevels
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

  // TODO Dedup Почти весь код ниже - копия метода AgpBuilder.installNode().
  // Наверное надо брать только самую последнюю точку размещения и с ней плясать в installNode().
  // А не накатывать всем покупками такого типа. См.TODO по installSql() ниже.


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    // Интересуют только item'ы определённого типа...
    lazy val logPrefix = s"LAM.installNode(${System.currentTimeMillis}):"

    val itype = _ITYPE
    val (gItems, other) = items.partition { i =>
      // Интересуют только item'ы adn-map с шейпами.
      i.iType == itype && {
        val r = i.geoShape.isDefined
        if (!r)
          LOGGER.error(s"$logPrefix Invalid AdnMap item: geoShape is missing:\n $i")
        r
      }
    }
    val this2 = super.installNode(other)

    // При сборке эджей считаем, что происходит пересборка эджей с нуля.
    if (gItems.nonEmpty) {

      // Аккамулируем все item'ы для единого эджа.
      val (geoShapes, _) = gItems
        .foldLeft( List.empty[MEdgeGeoShape] -> MEdgeGeoShape.SHAPE_ID_START ) {
          case ((acc, counter), mitem) =>
            val meGs = MEdgeGeoShape(
              id      = counter,
              glevel  = NodeGeoLevels.geoPlace,
              shape   = mitem.geoShape.get
            )
            (meGs :: acc) -> (counter + 1)
        }

      // Надо собрать опорные точки для общей статистики, записав их рядышком.
      // По идее, все шейпы - это PointGs.
      val geoPoints = di.advBuilderUtil
        .grabGeoPoints4Stats( gItems )
        .toSeq

      // Собираем единый эдж для геолокации карточки в месте на гео.карте.
      val e = MEdge(
        predicate = _PRED,
        info = MEdgeInfo(
          geoShapes = geoShapes,
          geoPoints = geoPoints
        )
      )

      LOGGER.trace(s"$logPrefix Found ${gItems.size} items for adv-geo-place: ${geoShapes.size} geoshapes, ${geoPoints.size} geo points.")

      // Собрать новую карточку, аккамулятор, билдер...
      this2.withAccUpdated { acc0 =>
        acc0.copy(
          mad = acc0.mad.copy(
            edges = acc0.mad.edges.copy(
              out = {
                val iter = acc0.mad.edges.iterator ++ Seq(e)
                MNodeEdges.edgesToMap1(iter)
              }
            )
          )
        )
      }


    } else {
      this2
    }
  }


  // TODO Запилить installSql, который откатывает все прошлые размещения, возвращает с них неизрасходованное бабло назад.

}
