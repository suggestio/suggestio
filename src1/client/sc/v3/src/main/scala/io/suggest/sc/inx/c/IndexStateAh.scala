package io.suggest.sc.inx.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.inx.m.MScIndexState
import io.suggest.sc.m.ScreenReset

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 18:37
  * Description: Контроллер внутренних базовых параметров выдачи.
  */
class IndexStateAh[M](
                       modelRW: ModelRW[M, MScIndexState]
                     )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Изменился экран текущего устройства.
    case ScreenReset =>
      val v2 = value.withScreen( JsScreenUtil.getScreen )
      updated( v2 )

  }

}
