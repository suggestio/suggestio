package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.ad.edit.m.pop.MAePopupsS
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.lk.m.{CloseAllPopups, DocBodyClick, ErrorPopupCloseClick}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 11:21
  * Description: Хвостовой ActionHandler, т.е. перехватывает всякие необязательные к обработке экшены.
  */
class TailAh[M](modelRW: ModelRW[M, MAeRoot]) extends ActionHandler(modelRW) {

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Перехват ненужного события клика в документе.
    case DocBodyClick =>
      noChange

    // Клик по кнопке закрытия попапа ошибки.
    case ErrorPopupCloseClick =>
      val v0 = value
      val v2 = v0.withPopups(
        v0.popups
          .withErrors( None )
      )
      updated( v2 )

    // Закрытие всех попапов.
    case CloseAllPopups =>
      val v0 = value
      val v2 = v0.withPopups(
        MAePopupsS.empty
      )
      updated( v2 )

  }

}
