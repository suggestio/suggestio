package io.suggest.common.maps.rad

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 16:09
  * Description:
  */
object RadConst {

  private def ID_PREFIX = "mrf_"

  def IMG_ID_RADIUS_MARKER  = ID_PREFIX + "irm"

}


/** Интерфейс для констант ограничения радиусов или других min/max в метрах. */
trait IMinMaxM {

  /** Минимальное значение радиуса в метрах. */
  def MIN_M: Int

  /** Максимальное значение радиуса в метрах. */
  def MAX_M: Int

}

