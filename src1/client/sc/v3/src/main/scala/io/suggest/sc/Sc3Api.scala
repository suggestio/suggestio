package io.suggest.sc

import io.suggest.sc.u.api.{IScUniApi, ScUniApiHttpImpl}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 15:39
  * Description: API выдачи.
  */

/** Интерфейс полного API. Не ясно, нужен ли. */
trait ISc3Api
  extends IScUniApi


/** XHR-реализация API. */
class Sc3ApiXhrImpl
  extends ScUniApiHttpImpl
  with ISc3Api

