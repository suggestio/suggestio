package util.captcha

import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import io.suggest.util.SioRandom
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.ArrayUtils
import scala.util.Random
import java.security.{MessageDigest, Security}
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.06.14 18:18
 * Description: Утиль для работы с капчами. Разгаданные значения капч лежат зашифрованными в куке,
 * которая имеет короткий цикл жизни, и имя её -- некий рандомный id капчи, который передаётся вместе с сабмитом формы,
 * к которой относится эта капча. Т.е. рандомный id генерится с формой, но кука с капчей выставляется только при
 * получении картинки капчи.
 * При сабмите значение капчи расшифровывается и сравнивается с выхлопом юзера через простое API.
 */


/** Утиль для криптографии, используемой при stateless-капчевании. */
object CipherUtil {

  // TODO следует свалить с дефолтовых алгоритмов (AES) на какую-нибудь маргинальщину из bouncy-castle.
  // TODO CBC требует задавать IV, что довольно неудобно при stateless. Но можно заюзать хеш от User-Agent для IV вместе с какими-то ещё данными.
  val CIPHER_SPEC = "AES/CBC/PKCS7Padding"
  val SECRET_KEY_ALGO = "AES"


  /** PKCS7Padding есть только в BouncyCastle, поэтому нужно дергать функцию регистрации JCE-провайдера
    * BC при старте системы или тестов. */
  def ensureBcJce() {
    Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) match {
      case null => Security.addProvider(new BouncyCastleProvider)
      case _ => // do nothing
    }
  }


  def getCipherInstance = {
    // TODO Явный вызов bcprov через JCE не помог решить проблемы с US Export policy. Надо бы что-то придумать.
    //Cipher.getInstance(CIPHER_SPEC, BouncyCastleProvider.PROVIDER_NAME)
    Cipher.getInstance(CIPHER_SPEC)
  }


  def generateSecretKey(bitLen: Int = 256, rnd: Random = SioRandom.rnd): Array[Byte] = {
    val arr = Array.fill[Byte](bitLen / 8)(0)
    rnd.nextBytes(arr)
    arr
  }

  /** При использовании CBC нужен IV, который выводится из разного барахла, в т.ч. из статических рандомных байт. */
  private val IV_MATERIAL_DFLT = {
    Array[Byte](-112, 114, -62, 99, -19, -86, 118, -42, 77, -103, 33, -30, -91, 104, 18, -105,
                101, -39, 4, -41, 24, -79, 58, 58, -7, -119, -68, -42, -102, 53, -104, -33)
  }


  /** Секретный ключ симметричного шифра. Сгенерить новый можно через generateSecretKey(). */
  // TODO В будущем следует придумать ротацию секретных ключей, чтобы генерились и ротировались во времени.
  private val SECRET_KEY = {
    Array[Byte](-22, 52, -78, -47, -46, 44, -3, 116, -8, -2, -96, -98, 48, 102, -117, -43,
                -59, -23, 75, 59, -101, 21, -26, 51, -102, -76, 22, 43, -94, -43, 111, 51)
  }

  private def mixWithIvDflt(iv0: Array[Byte]): Array[Byte] = {
    val longIv = if (iv0.length == 0)
      IV_MATERIAL_DFLT
    else
      ArrayUtils.addAll(iv0, IV_MATERIAL_DFLT : _*)
    // Нужно укороить iv до 16 байт, заодно скрыв его. Это можно сделать через md5() или другой 128-битный хеш.
    MessageDigest.getInstance("MD5").digest(longIv)
  }

  def encryptPrintable(str2enc: String, ivMaterial: Array[Byte] = Array.empty): String = {
    try {
      val cipher = getCipherInstance
      val secretKey = new SecretKeySpec(SECRET_KEY, SECRET_KEY_ALGO)
      val iv1 = mixWithIvDflt(ivMaterial)
      val ivSpec = new IvParameterSpec(iv1)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
      val cryptoBytes = cipher.doFinal(str2enc.getBytes)
      Base64.encodeBase64URLSafeString(cryptoBytes)
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Unable to encrypt string: " + str2enc, ex)
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
