package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.11.14 11:30
 * Description: Упрощенный доступ к некоторым
 */
package object ai {

  type MAiMadContentHandler   = MAiMadContentHandlers.MAiMadContentHandler

  type WindDirection          = GeoDirections.GeoDirection

  type SkyState               = SkyStates.SkyState
  type Precipation            = Precipations.Precipation

}
