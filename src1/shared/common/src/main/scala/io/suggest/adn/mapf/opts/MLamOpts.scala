package io.suggest.adn.mapf.opts

import boopickle.Default._
import io.suggest.common.empty.IsEmpty

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.05.17 16:12
  * Description: Модель опций Lam-формы.
  */

object MLamOpts {

  /** Поддержка boopickle. */
  implicit val mLamOptsP = generatePickler[MLamOpts]

}


/** Модель флагов формы.
  *
  * @param onAdvMap Состояние галочки размещения на карте рекламодателей.
  * @param onGeoLoc Состояние галочки размещения на карте геолокации.
  */
case class MLamOpts(
                     onAdvMap : Boolean,
                     onGeoLoc : Boolean
                   )
  extends IsEmpty
{

  override def nonEmpty = onAdvMap || onGeoLoc

  def withOnAdvMap(onAdvMap: Boolean) = copy(onAdvMap = onAdvMap)
  def withOnGeoLoc(onGeoLoc: Boolean) = copy(onGeoLoc = onGeoLoc)

}

