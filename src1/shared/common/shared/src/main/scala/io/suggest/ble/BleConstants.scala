package io.suggest.ble

import io.suggest.common.html.HtmlConstants
import io.suggest.common.uuid.LowUuidUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:51
  * Description: Константы для Bluetooth LE, для маячков изначально.
  */
object BleConstants {

  /** Строка с четырьмя нулями */
  val FOUR_ZEROES: String = {
    "0" * 4
  }

  /**
    * Какой-то суффикс для UUID сервиса BLE.
    * У блютуса какой-то велосипед с сервисами, т.е. байт слишком много и их постоянно удлиняют-укорачивают,
    * то подгоняя под UUID, то под нормальное число.
    * @return Строковой suffix UUID в lower case.
    */
  val SERVICES_BASE_UUID_LC: String = {
    val d = LowUuidUtil.UID_PARTS_DELIM
    d + FOUR_ZEROES + d + "1000-8000-00805F9B34FB"
  }

  /** Сборка полного UUID на основе 16-бит service id (hex). */
  def mkFullUuid(uuid16blc: String): String = {
    FOUR_ZEROES + uuid16blc + SERVICES_BASE_UUID_LC
  }


  /** Константы ble-маячков. */
  object Beacon {

    /** Разделитель частей UID. Для ibeacon нужно отделять uuid от major и minor с помощью этой строки: */
    final def UID_DELIM = HtmlConstants.UNDERSCORE


    /** Константы названий URL QS полей у BLE-маячков. */
    object Qs {

      /** Имя поля со строковым идентификатором маячка. В случае ibeacon - proximity uuid не уникален. */
      final def UID_FN            = "a"

      /** Имя поля с оценочным расстоянием до маячка. */
      final def DISTANCE_CM_FN     = "g"

    }


    object EddyStone {

      /** Короткий bt service UUID. */
      final def SERVICE_UUID_16B_LC = "feaa"
      final def SERVICE_UUID_FULL_LC = mkFullUuid(SERVICE_UUID_16B_LC)

      /** id маячка eddystone-uid. Совпадает с техническим id существующего маячка. */
      final def EXAMPLE_UID = "aa112233445566778899-000000000456"

    }

  }

}
