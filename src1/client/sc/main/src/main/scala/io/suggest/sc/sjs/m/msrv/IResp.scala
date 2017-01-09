package io.suggest.sc.sjs.m.msrv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.16 17:57
  * Description: Интерфейсы ответов сервера.
  */

/** Ответ сервера. */
trait IResp

/** Ответ сервера на focused-запрос. */
trait IFocResp extends IResp
