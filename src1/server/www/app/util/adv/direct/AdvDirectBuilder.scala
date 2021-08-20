package util.adv.direct

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.n2.edge._
import util.adv.build.IAdvBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.16 21:23
  * Description: adv-билдер bill2-mitem прямых размещений на узлах.
  */
object AdvDirectBuilder {

  private final def ITYPE = MItemTypes.AdvDirect
  private final def PRED  = MPredicates.Receiver.AdvDirect

  /** Точный список порождаемых этим билдером предикатов. */
  def PREDICATES_STRICT: List[MPredicate] = {
    PRED :: Nil
  }

  /** Список предикатов для полнейшей очистки direct-размещений. */
  def PREDICATES_FULL: List[MPredicate] = {
    PRED.parent.value :: Nil
  }

}

trait AdvDirectBuilder extends IAdvBuilder {

  import di._
  import AdvDirectBuilder._

  override def supportedItemTypes: List[MItemType] = {
    ITYPE :: super.supportedItemTypes
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
      val acc2 = advBuilderUtil
        .acc_node_edges_LENS
        .modify { edges0 =>
          // Собрать новую карту эджей.
          MNodeEdges.out.set(
            MNodeEdges.edgesToMap1(
              edges0
                .withoutPredicateIter( predsForClear : _* )
            )
          )(edges0)
        }(acc0)

      LOGGER.debug(s"clearNode($full): Cleared node#${acc0.mnode.idOrNull} ${acc0.mnode.edges.out.size}=>${acc2.mnode.edges.out.size} edges from predicates: [${predsForClear.mkString(", ")}]")
      acc2
    }
    withAcc( accFut2 )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    val (ditems, others) = advBuilderUtil.partitionItemsByType(items, ITYPE)

    val this2 = super.installNode(others)

    if (ditems.isEmpty) {
      this2
    } else {
      // Группировать по параметрам эджа, потом каждую группу перегонять в единственный эдж. Это снизит кол-во эджей.
      lazy val logPrefix = s"ADB.installNode()#${System.currentTimeMillis()}:"
      LOGGER.debug(s"$logPrefix Found ${ditems.size} for direct-adv building: ${ditems.iterator.flatMap(_.id).mkString(", ")}")

      // Группировка особо не требуется. Пихаем все receiver'ы в один эдж.
      val e = MEdge(
        predicate = MPredicates.Receiver.AdvDirect,
        nodeIds   = ditems
          .iterator
          .flatMap(_.rcvrIdOpt)
          .toSet
      )
      LOGGER.trace(s"$logPrefix Built new edge: item ##[${ditems.flatMap(_.id).mkString(",")}] => $e")

      this2.withAccUpdated { acc0 =>
        val acc2 = advBuilderUtil
          .acc_node_edges_out_LENS
          .modify(_ :+ e)(acc0)
        LOGGER.debug(s"$logPrefix Edges count changed: ${acc0.mnode.edges.out.size} => ${acc2.mnode.edges.out.size}. 1 edge created:\n $e")
        acc2
      }
    }
  }

}
