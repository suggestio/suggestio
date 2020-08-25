package io.suggest.sc.m.dia

import io.suggest.lk.nodes.form.LkNodesFormCircuit
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 12:19
  * Description: Модель данных выдачи для формы узлов.
  */
object MScNodes {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScNodes] = UnivEq.derive

  def circuit = GenLens[MScNodes]( _.circuit )

}


/** Контейнер данных формы управления узлами.
  *
  * @param circuit Логика lk-nodes-формы.
  */
final case class MScNodes(
                           circuit        : Option[LkNodesFormCircuit]        = None,
                         )
