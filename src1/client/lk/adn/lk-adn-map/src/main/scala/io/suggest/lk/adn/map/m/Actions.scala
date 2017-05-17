package io.suggest.lk.adn.map.m

import io.suggest.primo.IApply1
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 11:44
  * Description: Экшены Lk-adn-map-формы.
  */
sealed trait ILamAction extends DAction


/** Враппер над IApply1 для упрощённого описания объектов-компаньонов к Boolean-сигналам. */
sealed trait IApply1BoolTo[Res] extends IApply1 {
  override type ApplyArg_t = Boolean
  override type T = Res
}


/** Изменилось состояние опции размещения на карте рекламодателей. */
case class OnAdvsMapChanged(checked: Boolean) extends ILamAction
object OnAdvsMapChanged extends IApply1BoolTo[OnAdvsMapChanged]


/** Изменилось состояние опции размещения на карте геолокации. */
case class OnGeoLocChanged(checked: Boolean) extends ILamAction
object OnGeoLocChanged extends IApply1BoolTo[OnGeoLocChanged]
