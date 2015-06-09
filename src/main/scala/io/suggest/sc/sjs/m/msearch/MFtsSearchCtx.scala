package io.suggest.sc.sjs.m.msearch

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 17:46
 * Description: Состояние полнотекстового поиска. Поддерживается внутри FSM.
 */
case class MFtsSearchCtx(
  q           : Option[String]  = None,
  generation  : Long            = System.currentTimeMillis(),
  reqTimerId  : Option[Int]     = None,
  requested   : Boolean         = false
)
