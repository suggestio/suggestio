package io.suggest.ble

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

  def DIST_CM_FAR   = 20000


  /** Нормализация/оценка дистанции: подходит для подавления флуктуаций от находящихся рядом маячков. */
  def quantedDistance(dCm: Int): Int = {
    Math.sqrt( dCm )
      .toInt
  }


  object EddyStone {

    def ID_DELIM        = "-"

    def NS_ID_HEX_LEN   = 20
    def INST_ID_HEX_LEN = 12
    def NODE_ID_LEN     = NS_ID_HEX_LEN + ID_DELIM.length + INST_ID_HEX_LEN

    /** Regexp для проверки на EddyStone в нижнем регистре. */
    def EDDY_STONE_NODE_ID_RE_LC: String = {
      val hexCharRe = "[a-f0-9]"
      val pre = "{"
      val post = "}"
      hexCharRe + pre + NS_ID_HEX_LEN + post + ID_DELIM + hexCharRe + pre + INST_ID_HEX_LEN + post
    }

  }

}
