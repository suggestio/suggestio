package io.suggest.n2.edge.edit.m

import diode.FastEq
import io.suggest.n2.edge.MEdge
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:51
  * Description: Корневая модель формы редактирования эджа.
  */

object MEdgeEditRoot {

  implicit object EdgeEditRootFastEq extends FastEq[MEdgeEditRoot] {
    override def eqv(a: MEdgeEditRoot, b: MEdgeEditRoot): Boolean = {
      (a.edge ===* b.edge) &&
      (a.conf ===* b.conf) &&
      (a.edit ===* b.edit)
    }
  }

  @inline implicit def univEq: UnivEq[MEdgeEditRoot] = UnivEq.derive

  val edge = GenLens[MEdgeEditRoot](_.edge)
  val conf = GenLens[MEdgeEditRoot](_.conf)
  val edit = GenLens[MEdgeEditRoot](_.edit)


  implicit class EdgeEditRootExt( val mroot: MEdgeEditRoot ) extends AnyVal {

    /** Сборка обновлённого инстанса эджа. */
    def toEdgeIfUpdated: Option[MEdge] = {
      var edgeUpdAcc = List.empty[MEdge => MEdge]
      val e0 = mroot.edge

      val nodeIds2 = mroot.edit.nodeIds.toSet
      if (nodeIds2 !=* e0.nodeIds)
        edgeUpdAcc ::= (MEdge.nodeIds set nodeIds2)

      Option.when( edgeUpdAcc.nonEmpty ) {
        edgeUpdAcc.reduce(_ andThen _)(e0)
      }
    }

    def toEdge: MEdge =
      toEdgeIfUpdated getOrElse mroot.edge

  }

}


/** Контейнер данных состояния формы редактирования эджа.
  *
  * @param edge Редактируемый сейчас эдж.
  * @param conf Конфигурация.
  * @param edit Разные состояния под-редакторов.
  */
case class MEdgeEditRoot(
                          edge      : MEdge,
                          conf      : MNodeEdgeIdQs,
                          edit      : MEdgeEditS,
                        )
