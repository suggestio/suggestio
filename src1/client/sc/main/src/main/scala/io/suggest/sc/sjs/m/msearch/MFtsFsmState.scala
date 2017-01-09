package io.suggest.sc.sjs.m.msearch

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.06.15 10:12
 * Description: Внутреннее состояние полнотекстового поиска.
 */

object MFtsFsmState {

  def getGeneration: Long = System.currentTimeMillis()

}


case class MFtsFsmState(
  q               : String        = "",
  generation      : Long          = MFtsFsmState.getGeneration,
  reqTimerId      : Option[Int]   = None
)

