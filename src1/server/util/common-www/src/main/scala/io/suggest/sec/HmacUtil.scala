package io.suggest.sec

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 10:47
  * Description: Утиль для HMAC/MAC.
  *
  * @see [[https://en.wikipedia.org/wiki/HMAC Hash-based message authentication code]]
  */
object HmacUtil {

  /** Сборка инстанса MAC для алгоритма и строки-ключа.
    *
    * @param algo Алгоритм. См. [[HmacAlgos]].
    * @param secretKey Секретный ключ.
    * @return Mac
    */
  def mkMac(algo: String, secretKey: String): Mac = {
    mkMac(algo, secretKey.getBytes())
  }
  def mkMac(algo: String, secretKeyBytes: Array[Byte]): Mac = {
    val mac = Mac.getInstance(algo)
    val sks = new SecretKeySpec(secretKeyBytes, algo)
    mac.init(sks)
    mac
  }

}


/** Названия HMAC-алгоритмов, понятные для javax.crypto. */
object HmacAlgos {

  def HMAC_SHA1 = "HmacSHA1"

}

