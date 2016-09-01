package io.suggest.ble

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:51
  * Description: Константы для Bluetooth LE, для маячков изначально.
  */
object BleConstants {

  /** Константы ble-маячков. */
  object Beacon {

    /** Константы названий URL QS полей у BLE-маячков. */
    object Qs {

      def UUID_FN           = "u"
      def MAJOR_FN          = "j"
      def MINOR_FN          = "i"

      /** Мощность сигнала по данным bluetooth на принимающем LE-устройстве. */
      def SIG_POWER_FN      = "p"

      /** Мощность сигнала маячка на расстоянии 1 метр по мнению самого маячка. */
      def SIG_POWER_1M_FN   = "n"

    }
  }

}
