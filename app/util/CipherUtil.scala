package util

import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import io.suggest.util.SioRandom
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.ArrayUtils
import scala.util.Random
import java.security.{SecureRandom, MessageDigest, Security}
import org.bouncycastle.jce.provider.BouncyCastleProvider
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.14 15:12
 * Description: Утиль для доступа к удобным фунцкиям шифрования.
 */
object CipherUtil {

  // CBC требует задавать IV, что довольно неудобно при stateless. Но можно заюзать хеш от User-Agent для IV вместе с какими-то ещё данными.
  val CIPHER_SPEC_DFLT = "AES/CBC/PKCS7Padding"
  val SECRET_KEY_ALGO_DFLT = "AES"

  /** PKCS7Padding есть только в BouncyCastle, поэтому нужно дергать функцию регистрации JCE-провайдера
    * BC при старте системы или тестов. */
  def ensureBcJce() {
    Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) match {
      case null => Security.addProvider(new BouncyCastleProvider)
      case _ => // do nothing
    }
  }


  def generateSecretKey(bitLen: Int = 256, rnd: SecureRandom = new SecureRandom()): Array[Byte] = {
    val arr = Array.fill[Byte](bitLen / 8)(0)
    rnd.nextBytes(arr)
    arr
  }

}

import CipherUtil._

trait CipherUtilAddon {

  def CIPHER_SPEC = CIPHER_SPEC_DFLT
  def SECRET_KEY_ALGO = SECRET_KEY_ALGO_DFLT

  def getCipherInstance = {
    Cipher.getInstance(CIPHER_SPEC)
  }

  /** При использовании CBC нужен IV, который выводится из разного барахла, в т.ч. из статических рандомных байт. */
  protected def IV_MATERIAL_DFLT: Array[Byte]

  /** Секретный ключ симметричного шифра. Сгенерить новый можно через generateSecretKey(). */
  protected def SECRET_KEY: Array[Byte]


  private def mixWithIvDflt(iv0: Array[Byte]): Array[Byte] = {
    val longIv = if (iv0.length == 0)
      IV_MATERIAL_DFLT
    else
      ArrayUtils.addAll(iv0, IV_MATERIAL_DFLT : _*)
    // Нужно укороить iv до 16 байт, заодно скрыв его. Это можно сделать через md5() или другой 128-битный хеш.
    MessageDigest.getInstance("MD5").digest(longIv)
  }

  def encryptPrintable(str2enc: String, ivMaterial: Array[Byte]): String = {
    encryptPrintable(str2enc.getBytes, ivMaterial)
  }

  def encryptPrintable(data2enc: Array[Byte], ivMaterial: Array[Byte]): String = {
    try {
      val cipher = getCipherInstance
      val secretKey = new SecretKeySpec(SECRET_KEY, SECRET_KEY_ALGO)
      val iv1 = mixWithIvDflt(ivMaterial)
      val ivSpec = new IvParameterSpec(iv1)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
      val cryptoBytes = cipher.doFinal(data2enc)
      Base64.encodeBase64URLSafeString(cryptoBytes)
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Unable to encrypt data: " + data2enc, ex)
    }
  }

  def decryptPrintable(str2dec: String, ivMaterial: Array[Byte] = Array.empty): String = {
    try {
      val cipher = getCipherInstance
      val secretKey = new SecretKeySpec(SECRET_KEY, SECRET_KEY_ALGO)
      val iv1 = mixWithIvDflt(ivMaterial)
      val ivSpec = new IvParameterSpec(iv1)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
      val cryptoBytes = Base64.decodeBase64(str2dec)
      val sourceBytes = cipher.doFinal(cryptoBytes)
      new String(sourceBytes)
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Unable to decrypt string: " + str2dec, ex)
    }
  }

}
