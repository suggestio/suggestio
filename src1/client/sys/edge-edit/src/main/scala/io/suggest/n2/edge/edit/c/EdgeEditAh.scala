package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.n2.edge.MEdge
import io.suggest.n2.edge.edit.m.PredicateChanged
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:58
  * Description: Контроллер заливки файла в форме.
  */
class EdgeEditAh[M](
                     modelRW: ModelRW[M, MEdge],
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Смена предиката.
    case m: PredicateChanged =>
      val v0 = value
      if (m.pred2 ==* v0.predicate) {
        noChange
      } else {
        val v2 = (MEdge.predicate set m.pred2)(v0)
        updated(v2)
      }

  }

}
