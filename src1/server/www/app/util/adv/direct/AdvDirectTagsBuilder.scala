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
    lazy val logPrefix = s"${classOf[AdvDirectTagsBuilder].getSimpleName}.installNode(${System.currentTimeMillis}):"

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
              // Find tag node by tag face. On install phase, map of tags is build using tagFaces as keys.
              val tagNodeOpt = ctxOuter.tagNodesMap.get(tagFace)

              val edgeNodeIds = (for {
                mitem <- tagItems.iterator
                rcvrId <- mitem.rcvrIdOpt.iterator
                tagNodeId = tagNodeOpt
                  .orElse {
                    // Lookup for tag node using tagNodeIdOpt of current item. On unInstall phase, tagNodesMap is keyed by tagNode.id.
                    mitem
                      .tagNodeIdOpt
                      .flatMap( ctxOuter.tagNodesMap.get )
                  }
                  .flatMap(_.id)
                  .getOrElse {
                    throw new IllegalStateException(s"$logPrefix No valid tag-node found item#${mitem.id.orNull} for tag[[$tagFace]] on rcvr#$rcvrId in tagNodesMap[${ctxOuter.tagNodesMap.size}]=[${ctxOuter.tagNodesMap.keys.mkString("|")}];\n Known tag ids=[${tagItems.iterator.flatMap(_.tagNodeIdOpt).toSet.mkString(", ")}] of tag items list:\n -${tagItems.mkString("\n -")}")
                  }
              } yield {
                rcvrId :: tagNodeId :: Nil
              })
                .flatten
                .toSet

              MEdge(
                predicate = pred,
                nodeIds   = edgeNodeIds,
                info = MEdgeInfo(
                  tags = Set.empty + tagFace,
                ),
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
    advBuilderUtil.tagsInstallSql(this, items, _ITYPE, super.installSql )
  }

}
