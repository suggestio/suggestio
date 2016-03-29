package util.adv.geo.tag

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import io.suggest.ym.model.NodeGeoLevels
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.16 15:15
  * Description: Трейт поддержки adv-билдинга для геотеггов.
  */
trait AgtBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._

  private def _PRED   = MPredicates.TaggedBy.Agt
  private def _ITYPE  = MItemTypes.GeoTag


  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  /** Спиливание всех тегов, привязанных через биллинг.
    *
    * @param full ignored.
    */
  override def clearAd(full: Boolean): IAdvBuilder = {
    // Вычистить теги из эджей карточки
    val acc2Fut = for {
      acc0 <- super.clearAd(full).accFut
    } yield {
      val mad2 = acc0.mad.copy(
        edges = acc0.mad.edges.copy(
          out = {
            val p = _PRED
            val iter = acc0.mad
              .edges
              .iterator
              // Все теги и геотеги идут через биллинг. Чистка равносильна стиранию всех эджей TaggedBy.
              .filter( _.predicate != p )
            MNodeEdges.edgesToMap1( iter )
          }
        )
      )
      // Сохранить почищенную карточку в возвращаемый акк.
      acc0.copy(
        mad = mad2
      )
    }
    withAcc( acc2Fut )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {

    lazy val logPrefix = s"AGT.installNode(${System.currentTimeMillis}):"

    val itype = _ITYPE
    val (tagItems, other) = items.partition { i =>
      // Интересуют только item'ы тегов, у которых всё правильно оформлено.
      i.iType == itype && {
        val r = i.geoShape.isDefined && i.tagFaceOpt.isDefined
        if (!r)
          LOGGER.error(s"$logPrefix Invalid AGT item: one or more required fields are empty:\n $i")
        r
      }
    }
    val this2 = super.installNode(other)

    // При сборке эджей считаем, что карточка уже была заранее очищена от предыдущих тегов.
    // Это особенность новой архитектуры: всё перенакатывается заново всегда.

    if (tagItems.nonEmpty) {
      // Теги отработать, группируя по шейпу. Т.е. размещать как на форме размещения в тегах.
      // Индексировать дубликаты тегов внутри карточки легче для индексов, нежели избыточно индексировать шейпы.
      // Хотя это может вызвать неточности при аггрегации документов по тегам.
      val agtEdgesIter = tagItems
        .iterator
        .toSeq
        .groupBy(_.geoShape.get)
        // Конвертим группы в отдельные эджи.
        .iterator
        .map { case (gs, gsItems) =>
          MEdge(
            predicate = _PRED,
            info = MEdgeInfo(
              tags = gsItems.iterator.flatMap(_.tagFaceOpt).toSet,
              geoShapes = List(MEdgeGeoShape(
                id      = 1,
                glevel  = NodeGeoLevels.NGL_TOWN_DISTRICT,
                shape   = gs
              ))
            )
          )
        }

      this2.withAccUpdated { acc0 =>
        acc0.copy(
          mad = acc0.mad.copy(
            edges = acc0.mad.edges.copy(
              out = MNodeEdges.edgesToMap1( acc0.mad.edges.iterator ++ agtEdgesIter )
            )
          )
        )
      }

    } else {
      this2
    }
  }

}
