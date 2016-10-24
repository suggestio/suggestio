package io.suggest.common.radio

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.16 11:53
  * Description: Очень общая утиль для маячков.
  */
object BeaconUtil {

  /** Расстояние "прямо вот здесь" в см. */
  def DIST_CM_HERE  = 20

  /** Расстояние "рядом" в см. */
  def DIST_CM_2M    = 200

  /** 10 метров. */
  def DIST_CM_10M   = 1000

  /** 50 метров. */
  def DIST_CM_50M   = 5000

  def DIST_CM_FAR   = 10000

  /**
    * Сгруппировать по дистанциям.
    *
    * @param d расстояние до маячка в сантиметрах.
    * @return Группа маячка по расстоянию (DIST_CM_HERE..DIST_CM_FAR).
    */
  def distanceToDistGroup(d: Int): BeaconDistanceGroup_t = {
    if (d < DIST_CM_HERE) {
      DIST_CM_HERE
    } else if (d < DIST_CM_2M) {
      DIST_CM_2M
    } else if (d < DIST_CM_10M) {
      DIST_CM_10M
    } else if (d < DIST_CM_50M) {
      DIST_CM_50M
    } else {
      DIST_CM_FAR
    }
  }
  def distanceToDistGroup(bcn: BeaconData): BeaconDistanceGroup_t = {
    // В близи -- мелкая квантовка, в далеке -- не важно.
    distanceToDistGroup( bcn.distanceCm )
  }

}
