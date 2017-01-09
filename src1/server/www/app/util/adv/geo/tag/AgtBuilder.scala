package util.adv.geo.tag

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import io.suggest.ym.model.NodeGeoLevels
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.16 15:15
  * Description: Трейт поддержки adv-билдинга для гео-тегов.
  */
trait AgtBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._
  import slick.driver.api._

  private def _PRED   = MPredicates.TaggedBy.Agt
  private def _ITYPE  = MItemTypes.GeoTag


  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  override def clearNodePredicates: List[MPredicate] = {
    _PRED :: super.clearNodePredicates
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
      // Теги отработать, группируя по шейпу. Т.е. размещать в эджах карточек ровно как на форме размещения в тегах.
      // Индексировать дубликаты тегов внутри карточки легче для индексов, нежели избыточно индексировать шейпы.
      // Хотя это может вызвать неточности при аггрегации документов по тегам.
      // id узлов-тегов достаём из outer ctx.
      this2.withAccUpdatedFut { acc0 =>
        for (ctxOuter <- acc0.ctxOuterFut) yield {
          val agtEdgesIter = tagItems
            .iterator
            .toSeq
            .groupBy(_.geoShape.get)
            // Конвертим группы в отдельные эджи.
            .iterator
            .map { case (gs, gsItems) =>

              val tagFacesSet = gsItems
                .iterator
                .flatMap(_.tagFaceOpt)
                .toSet

              // Надо собрать опорные точки для общей статистики, записав их рядышком.
              val geoPoints = di.advBuilderUtil
                .grabGeoPoints4Stats( gsItems )
                .toSet
                .toSeq

              val nodeIdsSet = tagFacesSet.flatMap { tagFace =>
                val tnOpt = ctxOuter.tagNodesMap.get(tagFace)
                if (tnOpt.isEmpty)
                  LOGGER.warn(s"$logPrefix No tag-node found for face: $tagFace")
                tnOpt.flatMap(_.id)
              }

              MEdge(
                predicate = _PRED,
                nodeIds   = nodeIdsSet,
                info      = MEdgeInfo(
                  tags      = tagFacesSet,
                  geoShapes = List(MEdgeGeoShape(
                    id      = MEdgeGeoShape.SHAPE_ID_START,
                    glevel  = NodeGeoLevels.geoTag,
                    shape   = gs
                  )),
                  geoPoints = geoPoints
                )
              )
            }

          acc0.copy(
            mad = acc0.mad.copy(
              edges = acc0.mad.edges.copy(
                out = MNodeEdges.edgesToMap1(acc0.mad.edges.iterator ++ agtEdgesIter)
              )
            )
          )
        }
      }

    } else {
      this2
    }
  }


  /** Гео-теги требуют особой обработки с выставлением MItem.rcvrId. */
  override def installSql(items: Iterable[MItem]): IAdvBuilder = {

    val (ditems, others) = items.partition { i =>
      i.iType == MItemTypes.GeoTag
    }

    val this2 = super.installSql(others)

    // Собираем db-экшены для инсталляции
    if (ditems.nonEmpty) {
      lazy val logPrefix = s"AGT.installSql(${items.size}):"
      LOGGER.trace(s"$logPrefix There are ${ditems.size} geotags for install...")

      this2.withAccUpdatedFut { acc0 =>
        for (outerCtx <- acc0.ctxOuterFut) yield {
          val dbas1 = ditems.foldLeft(acc0.dbActions) { (dbas0, mitem) =>
            val dbAction = {
              val dateStart2 = now
              val dateEnd2 = dateStart2.plus(mitem.dtIntervalOpt.get.toPeriod)
              val mitemId = mitem.id.get
              // Определяем заодно id узла-тега. Это облегчит поиск в таблице на этапе перекомпиляции узлов-тегов.
              val rcvrIdOpt = mitem.tagFaceOpt
                .flatMap(outerCtx.tagNodesMap.get)
                .flatMap(_.id)

              if (rcvrIdOpt.isEmpty)
                LOGGER.warn(s"$logPrefix NOT found tag node id for tag-face: ${mitem.tagFaceOpt}")

              mItems.query
                .filter { _.id === mitemId }
                .map { i =>
                  (i.status, i.rcvrIdOpt, i.dateStartOpt, i.dateEndOpt, i.dateStatus)
                }
                .update((MItemStatuses.Online, rcvrIdOpt, Some(dateStart2), Some(dateEnd2), dateStart2))
                .filter { rowsUpdated =>
                  LOGGER.trace(s"$logPrefix Updated item[$mitemId] '${mitem.tagFaceOpt}': dateEnd => $dateEnd2, rcvrId => $rcvrIdOpt")
                  rowsUpdated == 1
                }
            }
            dbAction :: dbas0
          }
          acc0.copy(
            dbActions = dbas1
          )
        }
      }

    } else {
      this2
    }
  }

}
