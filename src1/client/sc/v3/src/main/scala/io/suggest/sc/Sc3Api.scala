package io.suggest.sc

import io.suggest.sc.c.grid.{FindAdsApiXhrImpl, IFindAdsApi}
import io.suggest.sc.c.inx.{IIndexApi, IndexApiXhrImpl}
import io.suggest.sc.c.search.{ISearchApi, SearchApiXhrImpl}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 15:39
  * Description: API выдачи.
  */

object Sc3Api {

  final def API_VSN = MScApiVsns.ReactSjs3

}


/** Интерфейс полного API. Не ясно, нужен ли. */
trait ISc3Api
  extends IIndexApi
  with IFindAdsApi
  with ISearchApi


/** XHR-реализация API. */
class Sc3ApiXhrImpl
  extends ISc3Api
  with IndexApiXhrImpl
  with FindAdsApiXhrImpl
  with SearchApiXhrImpl

