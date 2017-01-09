package io.suggest.common.radio

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.16 11:53
  * Description: Очень общая утиль для маячков.
  */
object BeaconUtil {

  // Разные оценочные расстояния до маячков.

  /** Расстояние "прямо вот здесь" в см. */
  def DIST_CM_HERE  = 20

  /** Расстояние "рядом" в см. */
  def DIST_CM_2M    = 200

  /** 10 метров. */
  def DIST_CM_10M   = 1000

  /** 50 метров. */
  def DIST_CM_50M   = 5000

  def DIST_CM_FAR   = 10000


  /** Нормализация/оценка дистанции: подходит для подавления флуктуаций от находящихся рядом маячков. */
  def quantedDistance(dCm: Int): Int = {
    Math.sqrt( dCm )
      .toInt
  }

}
