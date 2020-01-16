package io.suggest.n2.edge.edit.m

import diode.FastEq
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.2020 14:02
  * Description: Доп.состояния редакторов эджа.
  */
object MEdgeEditS {

  def empty = apply()

  implicit object MEdgeEditFastEq extends FastEq[MEdgeEditS] {
    override def eqv(a: MEdgeEditS, b: MEdgeEditS): Boolean = {
      (a.nodeIds ===* b.nodeIds)
    }
  }

  @inline implicit def univEq: UnivEq[MEdgeEditS] = UnivEq.derive

  val nodeIds = GenLens[MEdgeEditS](_.nodeIds)

}


/** Контейнер данных редактора эджа.
  *
  * @param nodeIds id узлов. Нужна Seq[], т.к. с Set[] будет постоянно перемешивание порядка id, затрудняя редактирование.
  */
case class MEdgeEditS(
                       nodeIds        : Seq[String]           = Nil,
                     )
