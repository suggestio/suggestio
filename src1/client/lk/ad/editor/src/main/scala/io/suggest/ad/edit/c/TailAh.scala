package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.DocBodyClick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 11:21
  * Description: Хвостовой ActionHandler, т.е. перехватывает всякие необязательные к обработке экшены.
  */
class TailAh[M, T](modelRW: ModelRW[M, T]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Перехват ненужного события клика в документе.
    case DocBodyClick =>
      noChange

  }

}
