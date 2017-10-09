package io.suggest.sc

import io.suggest.sc.inx.c.{IIndexApi, IndexApiXhrImpl}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 15:39
  * Description: API выдачи.
  */

object Sc3Api {

  final def API_VSN = ScConstants.Vsns.REACT_SJS

}


/** Интерфейс полного API. Не ясно, нужен ли. */
trait ISc3Api
  extends IIndexApi


/** XHR-реализация API. */
class Sc3ApiXhrImpl
  extends ISc3Api
  with IndexApiXhrImpl

