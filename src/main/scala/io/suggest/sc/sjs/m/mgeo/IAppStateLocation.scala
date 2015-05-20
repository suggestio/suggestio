package io.suggest.sc.sjs.m.mgeo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:04
 * Description: Доступ к имеющимся данным геолокации: вышкам, маякам, координатам gps и т.д.
 */
trait IAppStateLocation {

  /** Координаты геолокации согласно данным медународных систем геопозиционирования. */
  // TODO Модель MGeoMode имеет другой смысл, хоть и совместима. Нужно другую модель задействовать.
  def geoLoc: Option[MGeoModeLoc]

}


/** Дефолтовая реализация [[IAppStateLocation]]. */
case class MAppStateLocation(
  override val geoLoc: Option[MGeoModeLoc] = None
)
  extends IAppStateLocation
