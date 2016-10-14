package io.suggest.common.uuid

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 22:23
  * Description: Низкоуровневая утиль для UUID.
  */
object LowUuidUtil {

  /** Отформатировать обычную hex-строку в UUID путём вставки дефисов. */
  def hexStringToUuid(hexString: String): String = {
    if (hexString.length != 32) {
      throw new IllegalArgumentException( hexString )

    } else {
      // Это строка вида b9407f30f5f8466eaff925556b57fe6d. Пора навтыкать дефисов.
      val a1 = hexString.substring(0, 8)
      val a2 = hexString.substring(8, 12)
      val a3 = hexString.substring(12, 16)
      val a4 = hexString.substring(16, 20)
      val a5 = hexString.substring(20)
      val d = "-"
      a1 + d + a2 + d + a3 + d + a4 + d + a5
    }
  }

}
