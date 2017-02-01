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
      acc0.copy(
        mad = acc0.mad.copy(
          edges = acc0.mad.edges.copy(
            out = {
              // Полная чистка удаляет всех ресиверов. Обычная -- касается только AdvDirect.
              val preds = if (full) {
                PREDICATES_FULL
              } else {
                PREDICATES_STRICT
              }
              // Собрать новую карту эджей.
              MNodeEdges.edgesToMap1(
                acc0.mad.edges
                  .withoutPredicateIter( preds : _* )
              )
            }
          )
        )
      )
    }
    withAcc( accFut2 )
  }


  override def installNode(items: Iterable[MItem]): IAdvBuilder = {
    val itypes = SUPPORTED_TYPES    // Без .toSet, т.к. коллекция типов очень маленькая.
    val (ditems, others) = items.partition { i =>
      itypes.contains(i.iType)
    }

    val this2 = super.installNode(others)

    if (ditems.isEmpty) {
      this2
    } else {
      // Группировать тут по устаревшему show-levels, потом каждую группу перегонять в единственный эдж. Это снизит кол-во эджей.
      val edgesIter = ditems
        .toSeq
        .groupBy { i =>
          (i.iType, i.sls, i.tagFaceOpt)
        }
        .iterator
        .map { case ((iType, sls, tagFaceOpt), slsItems) =>
          MEdge(
            predicate = itypeToPredicate(iType),
            nodeIds   = slsItems.iterator.flatMap(_.rcvrIdOpt).toSet,
            info = MEdgeInfo(
              sls  = sls,
              tags = tagFaceOpt.toSet
            )
          )
        }

      this2.withAccUpdated { acc0 =>
        acc0.copy(
          mad = acc0.mad.copy(
            edges = acc0.mad.edges.copy(
              out = MNodeEdges.edgesToMap1( acc0.mad.edges.iterator ++ edgesIter )
            )
          )
        )
      }
    }
  }

}
