package util.adv.direct

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.n2.edge._
import japgolly.univeq._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.18 15:29
  * Description: Билдер direct-тегов.
  * Куски этого билдера недальновидно распихивались по другим билдерам, что оказалось недальновидно.
  */
trait AdvDirectTagsBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._

  private final def _PRED   = MPredicates.TaggedBy.DirectTag
  private final def _ITYPE  = MItemTypes.TagDirect


  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  override def clearNodePredicates: List[MPredicate] = {
    _PRED :: super.clearNodePredicates
  }


  /** Установка данного тега в карточку. */
  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    lazy val logPrefix = s"AGT.installNode(${System.currentTimeMillis}):"

    val itype = _ITYPE
    val (tagsItems, other) = items.partition { i =>
      // Интересуют только item'ы тегов, у которых всё правильно оформлено.
      i.iType ==* itype && {
        val r = i.rcvrIdOpt.isDefined && i.tagFaceOpt.isDefined
        if (!r)
          LOGGER.error(s"$logPrefix Invalid ADT item: one or more required fields are empty:\n $i")
        r
      }
    }

    val this2 = super.installNode(other)

    if (tagsItems.isEmpty) {
      this2

    } else {
      // Есть теги для прямой установки.
      LOGGER.debug(s"$logPrefix Found ${tagsItems.size} items for adv-direct-tag install: ${tagsItems.iterator.flatMap(_.id).mkString(",")}")

      this2.withAccUpdatedFut { acc0 =>
        for (ctxOuter <- acc0.ctxOuterFut) yield {
          val pred = _PRED
          // Теги обработать, группируя по tagId
          val directTagEdgesIter = tagsItems
            .groupBy(_.tagFaceOpt.get)
            .iterator
            .map { case (tagFace, tagItems) =>
              // Найти узел тега. Он или уже существует, или должен быть уже создан.
              val tagNodeOpt = ctxOuter.tagNodesMap.get(tagFace)
              if (tagNodeOpt.isEmpty)
                LOGGER.warn(s"$logPrefix No tag-node found for face: $tagFace")
              val tagNodeIdOpt = tagNodeOpt.flatMap(_.id)

              val edgeNodeIds = tagItems
                .iterator
                .flatMap(_.rcvrIdOpt)
                .++( tagNodeIdOpt )
                .toSet

              MEdge(
                predicate = pred,
                nodeIds   = edgeNodeIds,
                info = MEdgeInfo(
                  tags = Set(tagFace)
                )
              )
            }

          advBuilderUtil
            .acc_node_edges_out_LENS
            .modify(_ ++ directTagEdgesIter)(acc0)
        }
      }
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
