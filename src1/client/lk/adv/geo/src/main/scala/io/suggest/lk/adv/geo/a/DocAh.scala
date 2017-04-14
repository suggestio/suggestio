package io.suggest.lk.adv.geo.a

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.adv.geo.m.{DocReadMoreClick, MDocS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 22:44
  * Description: Action handler для компонента документации по форме георазмещения.
  */
class DocAh[M](
                modelRW: ModelRW[M, MDocS]
              )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал клика по ссылке-кнопке разворачивания подробной документации.
    case DocReadMoreClick =>
      updated( value.withExpanded(true) )

  }

}
