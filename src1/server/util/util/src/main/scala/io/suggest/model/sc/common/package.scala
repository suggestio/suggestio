package io.suggest.model.sc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 15:25
 */
package object common {

  type SinkShowLevel = SinkShowLevels.T

  type AdShowLevel   = AdShowLevels.T

  type LvlMap_t = Map[AdShowLevel, Int]

}
