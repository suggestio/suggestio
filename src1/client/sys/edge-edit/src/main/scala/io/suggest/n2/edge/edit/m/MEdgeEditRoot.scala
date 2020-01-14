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
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MEdgeEditRoot] = UnivEq.derive

  val edge = GenLens[MEdgeEditRoot](_.edge)
  val conf = GenLens[MEdgeEditRoot](_.conf)

}


/** Контейнер данных состояния формы редактирования эджа.
  *
  * @param edge Редактируемый сейчас эдж.
  * @param conf Конфигурация.
  */
case class MEdgeEditRoot(
                          edge      : MEdge,
                          conf      : MNodeEdgeIdQs,
                        )
