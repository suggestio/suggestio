package io.suggest.sc.search.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.hdr.m.HSearchBtnClick
import io.suggest.sc.search.m.{MScSearch, SwitchTab}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
class SearchAh[M](
                  modelRW: ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case HSearchBtnClick =>
      val v2 = value.withIsShown( true )
      updated( v2 )

    // Клик по кнопке закрытия поисковой выдачи.
    //case

    // Смена текущего таба на панели поиска.
    case m: SwitchTab =>
      val v2 = value.withCurrTab( m.newTab )
      updated( v2 )

  }

}
