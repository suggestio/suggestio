package util.adv.direct

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.model.n2.edge._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 21:23
  * Description: adv-билдер bill2-mitem прямых размещений на узлах.
  */
object AdvDirectBuilder {

  /** Поддерживаемые типы узлов. */
  def SUPPORTED_TYPES: List[MItemType] = {
    MItemTypes.AdvDirect ::
      MItemTypes.TagDirect ::
      Nil
  }

  /** Общий код сборки списка предикатов вынесен в этот метод. */
  def PREDICATES0: List[MPredicate] = {
    MPredicates.TaggedBy.DirectTag :: Nil
  }

  /** Точный список порождаемых этим билдером предикатов. */
  def PREDICATES_STRICT: List[MPredicate] = {
    MPredicates.Receiver.AdvDirect ::
      PREDICATES0
  }

  /** Список предикатов для полнейшей очистки direct-размещений. */
  def PREDICATES_FULL: List[MPredicate] = {
    MPredicates.Receiver ::
      PREDICATES0
  }

  /**
    * Маппер поддерживаемых типов item'ов на предикаты эджей размещения.
    */
  def itypeToPredicate(ntype: MItemType): MPredicate = {
    ntype match {
      case MItemTypes.AdvDirect           => MPredicates.Receiver.AdvDirect
      case MItemTypes.TagDirect           => MPredicates.TaggedBy.DirectTag

      // should never happen:
      case other =>
        throw new IllegalArgumentException("Unsupported item type: " + other)
    }
  }

}

trait AdvDirectBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._
  import AdvDirectBuilder._

  override def supportedItemTypes: List[MItemType] = {
    SUPPORTED_TYPES reverse_::: super.supportedItemTypes
  }


  /** Очистить все прямые размещения карточки по биллингу.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния.
    *
    * @param full true -- Вычистить всех ресиверов в т.ч. саморазмещение.
    *             false -- Вычистить только платных ресиверов.
    */
  override def clearNode(full: Boolean): IAdvBuilder = {
    val accFut2 = for {
      acc0 <- super.clearNode(full).accFut
    } yield {
      // Полная чистка удаляет всех ресиверов. Обычная -- касается только AdvDirect.
      val predsForClear = if (full) {
        PREDICATES_FULL
      } else {
        PREDICATES_STRICT
      }
      val mnode2 = acc0.mnode.withEdges(
        acc0.mnode.edges.copy(
          out = {
            // Собрать новую карту эджей.
            MNodeEdges.edgesToMap1(
              acc0.mnode.edges
                .withoutPredicateIter( predsForClear : _* )
            )
          }
        )
      )
      LOGGER.debug(s"clearNode($full): Cleared node#${acc0.mnode.idOrNull} ${acc0.mnode.edges.out.size}=>${mnode2.edges.out.size} edges from predicates: [${predsForClear.mkString(", ")}]")
      acc0.withMnode( mnode2 )
    }
    withAcc( accFut2 )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    val (ditems, others) = advBuilderUtil.partitionItemsByType(items, SUPPORTED_TYPES:_*)

    val this2 = super.installNode(others)

    if (ditems.isEmpty) {
      this2
    } else {
      // Группировать по параметрам эджа, потом каждую группу перегонять в единственный эдж. Это снизит кол-во эджей.
      lazy val logPrefix = s"ADB.installNode()#${System.currentTimeMillis()}:"
      LOGGER.debug(s"$logPrefix Found ${ditems.size} for direct-adv building: ${ditems.iterator.flatMap(_.id).mkString(", ")} ")

      val newEdges = ditems
        .toSeq
        .groupBy { i =>
          (i.iType, i.tagFaceOpt)
        }
        .iterator
        .map { case ((iType, tagFaceOpt), itemsGroup) =>
          val e = MEdge(
            predicate = itypeToPredicate(iType),
            nodeIds   = itemsGroup.iterator
              .flatMap( _.rcvrIdOpt )
              .toSet,
            info = MEdgeInfo(
              tags = tagFaceOpt.toSet
            )
          )
          LOGGER.trace(s"$logPrefix Built new edge: item t#$iType=>p#${e.predicate} ##[${itemsGroup.flatMap(_.id).mkString(",")}] => $e")
          e
        }
        .toStream

      this2.withAccUpdated { acc0 =>
        val mnode2 = acc0.mnode.withEdges(
          acc0.mnode.edges.copy(
            out = acc0.mnode.edges.out ++ newEdges
          )
        )
        LOGGER.debug(s"$logPrefix Edges count changed: ${acc0.mnode.edges.out.size} => ${mnode2.edges.out.size}. ${newEdges.size} edges created:\n ${newEdges.mkString(",\n ")}")
        acc0.withMnode( mnode2 )
      }
    }
  }

}
