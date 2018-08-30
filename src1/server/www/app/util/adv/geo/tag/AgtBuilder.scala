package util.adv.geo.tag

import io.suggest.geo.MNodeGeoLevels
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import japgolly.univeq._
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

  private final def _PRED   = MPredicates.TaggedBy.Agt
  private final def _ITYPE  = MItemTypes.GeoTag


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
      i.iType ==* itype && {
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
      LOGGER.debug(s"$logPrefix Found ${tagItems.size} items for adv-geo-tag install: ${tagItems.iterator.flatMap(_.id).mkString(",")}")

      // Теги отработать, группируя по шейпу. Т.е. размещать в эджах карточек ровно как на форме размещения в тегах.
      // Индексировать дубликаты тегов внутри карточки легче для индексов, нежели избыточно индексировать шейпы.
      // Хотя это может вызвать неточности при аггрегации документов по тегам.
      // id узлов-тегов достаём из outer ctx.
      this2.withAccUpdatedFut { acc0 =>
        for (ctxOuter <- acc0.ctxOuterFut) yield {
          val pred = _PRED
          val agtEdgesIter = tagItems
            .iterator
            .toSeq
            .groupBy(_.geoShape.get)
            // Конвертим группы в отдельные эджи.
            .iterator
            .map { case (gs, gsItems) =>
              // Сборка всех tag face'ов.
              val tagFacesSet = gsItems
                .iterator
                .flatMap(_.tagFaceOpt)
                .toSet

              // Надо собрать опорные точки для общей статистики, записав их рядышком.
              val geoPoints = di.advBuilderUtil
                .grabGeoPoints4Stats( gsItems )
                .toSet
                .toSeq

              val nodeIdsSet = tagFacesSet
                .iterator
                .flatMap { tagFace =>
                  val tnOpt = ctxOuter.tagNodesMap.get(tagFace)
                  val tnIdOpt = tnOpt.flatMap(_.id)
                  // Сообщать о проблеме с тегом: неправильно вызывать этот код, если узел тега ещё не существует. TODO Может сразу делать throw?
                  if (tnIdOpt.isEmpty)
                    LOGGER.error(s"$logPrefix No tag-node found for tag-face or _id missing: $tnOpt")
                  tnIdOpt
                }
                .toSet

              MEdge(
                predicate = pred,
                nodeIds   = nodeIdsSet,
                info      = MEdgeInfo(
                  tags      = tagFacesSet,
                  geoShapes = List(MEdgeGeoShape(
                    id      = MEdgeGeoShape.SHAPE_ID_START,
                    glevel  = MNodeGeoLevels.geoTag,
                    shape   = gs
                  )),
                  geoPoints = geoPoints
                )
              )
            }

          acc0.withMnode(
            mnode = acc0.mnode.withEdges(
              acc0.mnode.edges.copy(
                out = MNodeEdges.edgesToMap1(
                  acc0.mnode.edges.iterator ++ agtEdgesIter
                )
              )
            )
          )
        }
      }

    } else {
      this2
    }
  }


  /** Теги требуют обработки с выставлением MItem.tagNodeId. */
  override def installSql(items: Iterable[MItem]): IAdvBuilder = {
    val tagItemType = _ITYPE
    val (ditems, others) = items.partition { i =>
      i.iType ==* tagItemType
    }

    val this2 = super.installSql(others)

    // Собираем db-экшены для инсталляции
    if (ditems.nonEmpty) {
      advBuilderUtil.tagsInstallSql(this2, ditems)
    } else {
      this2
    }
  }

}
