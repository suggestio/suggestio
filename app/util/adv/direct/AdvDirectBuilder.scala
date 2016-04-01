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
trait AdvDirectBuilder extends IAdvBuilder {

  import di._
  import mCommonDi._

  /** Предикат, с которым работает этот трейт. */
  private def _PRED   = MPredicates.Receiver.AdvDirect
  /** Тип item'а, который фигурирует в работе этого трейта. */
  private def _ITYPE  = MItemTypes.AdvDirect

  override def supportedItemTypes: List[MItemType] = {
    _ITYPE :: super.supportedItemTypes
  }

  /** Очистить все прямые размещения карточки по биллингу.
    * Используется для рассчета состояния с нуля, вместо обновления существующего состояния.
    *
    * @param full true -- Вычистить всех ресиверов в т.ч. саморазмещение.
    *             false -- Вычистить только платных ресиверов.
    */
  override def clearAd(full: Boolean): IAdvBuilder = {
    val accFut2 = for {
      acc0 <- super.clearAd(full).accFut
    } yield {
      acc0.copy(
        mad = acc0.mad.copy(
          edges = acc0.mad.edges.copy(
            out = {
              // Полная чистка удаляет всех ресиверов. Обычная -- касается только AdvDirect.
              val p = if (full) MPredicates.Receiver else _PRED
              // Собрать новую карту эджей.
              MNodeEdges.edgesToMap1(
                acc0.mad.edges
                  .withoutPredicateIter(p)
              )
            }
          )
        )
      )
    }
    withAcc( accFut2 )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    val itype = _ITYPE
    val (ditems, others) = items.partition { i =>
      i.iType == itype && i.rcvrIdOpt.nonEmpty && i.sls.nonEmpty
    }

    val this2 = super.installNode(others)

    if (ditems.isEmpty) {
      this2
    } else {
      // TODO Opt Сделать medge.nodeIdOpt списком, группировать тут по show-levels, потом каждую группу перегонять в единственный эдж.
      val eiter = ditems
        .iterator
        .map { mitem =>
          MEdge(
            predicate = MPredicates.Receiver.AdvDirect,
            nodeIds   = mitem.rcvrIdOpt.toSet,
            info = MEdgeInfo(
              sls = mitem.sls
            )
          )
        }
      this2.withAccUpdated { acc0 =>
        acc0.copy(
          mad = acc0.mad.copy(
            edges = acc0.mad.edges.copy(
              out = MNodeEdges.edgesToMap1( acc0.mad.edges.iterator ++ eiter )
            )
          )
        )
      }
    }
  }

}
