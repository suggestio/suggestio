package io.suggest.common.radio

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:51
  * Description: Константы для Bluetooth LE, для маячков изначально.
  */
object BleConstants {

  /**
    * Какой-то суффикс для UUID сервиса BLE.
    * У блютуса какой-то велосипед с сервисами, т.е. байт слишком много и их постоянно удлиняют-укорачивают,
    * то подгоняя под UUID, то под нормальное число.
    * @return Строковой suffix UUID в lower case.
    */
  def SERVICES_BASE_UUID_LC = "-0000-1000-8000-00805f9b34fb"

  /** Гугломаячки исповедуют этот id сервиса или что-то в этом роде. */
  def EDDY_STONE_SERVICE_UUID_PREFIX_LC = "0000feaa"


  /** Константы ble-маячков. */
  object Beacon {

    /** Разделитель частей UID. Для ibeacon нужно отделять uuid от major и minor с помощью этой строки: */
    def UID_DELIM = "_"

    /** Разделитель, используемый при форматировании кусков ID. */
    def UID_PARTS_DELIM = "-"

    /** Константы названий URL QS полей у BLE-маячков. */
    object Qs {

      /** Имя поля со строковым идентификатором маячка. В случае ibeacon - proximity uuid не уникален. */
      def UID_FN            = "a"

      /** Имя поля с числом ibeacon major. */
      //def MAJOR_FN          = "b"

      /** Имя поля с числом ibeacon minor. */
      //def MINOR_FN          = "c"

      /** Мощность сигнала по данным bluetooth на принимающем LE-устройстве. */
      //def RSSI_FN           = "d"

      /** Мощность сигнала маячка на расстоянии distance0 метров по мнению производителя маячка. */
      //def RSSI0_FN          = "e"

      /** Калибровочная дистанция, на которой сигнал имеет RSSI = rssi0 */
      //def DISTANCE0_M_FN    = "f"

      /** Имя поля с оценочным расстоянием до маячка. */
      def DISTANCE_CM_FN     = "g"

    }

  }

}
