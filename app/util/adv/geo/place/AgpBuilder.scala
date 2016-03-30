package util.adv.geo.place

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemTypes, MItemType}
import io.suggest.model.n2.edge._
import io.suggest.ym.model.NodeGeoLevels
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


  override def clearAd(full: Boolean): IAdvBuilder = {
    di.advBuilderUtil.clearByPredicate(
      b0    = super.clearAd(full),
      pred  = _PRED
    )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    lazy val logPrefix = s"AGP.installNode(${System.currentTimeMillis}):"

    val itype = _ITYPE
    val (gItems, other) = items.partition { i =>
      // Интересуют только item'ы тегов, у которых всё правильно оформлено.
      i.iType == itype && {
        val r = i.geoShape.isDefined
        if (!r)
          LOGGER.error(s"$logPrefix Invalid AGP item: geoShape is missing:\n $i")
        r
      }
    }
    val this2 = super.installNode(other)

    // При сборке эджей считаем, что происходит пересборка эджей с нуля.
    if (gItems.nonEmpty) {

      LOGGER.trace(s"$logPrefix Found ${gItems.size} items for adv-geo-place.")

      // Аккамулируем все item'ы для единого эджа.
      val (megs, _) = gItems
        .foldLeft( List.empty[MEdgeGeoShape] -> MEdgeGeoShape.SHAPE_ID_START ) {
          case ((acc, counter), mitem) =>
            val meGs = MEdgeGeoShape(
              id      = counter,
              glevel  = NodeGeoLevels.NGL_TOWN_DISTRICT,
              shape   = mitem.geoShape.get
            )
            (meGs :: acc) -> (counter + 1)
        }

      // Собираем единый эдж для геолокации карточки в месте на гео.карте.
      val e = MEdge(
        predicate = MPredicates.AdvGeoPlace,
        info = MEdgeInfo(
          geoShapes = megs
        )
      )

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

}
