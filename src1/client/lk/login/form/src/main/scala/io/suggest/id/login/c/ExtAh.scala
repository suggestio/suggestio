package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.ExtLoginVia
import io.suggest.id.login.m.ext.MExtLoginFormS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 17:44
  * Description: Контроллер данных для внешнего логина.
  */
class ExtAh[M](
                modelRW: ModelRW[M, MExtLoginFormS]
              )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск логина через внешний сервис. Надо начать запрос ссылки.
    case m: ExtLoginVia =>

      ???

  }

}
