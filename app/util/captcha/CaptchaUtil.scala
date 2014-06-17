package util.captcha

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import io.suggest.util.SioRandom
import org.apache.commons.codec.binary.Base64
import scala.util.Random

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

  def generateSecretKey(bitLen: Int = 256, rnd: Random = SioRandom.rnd): Array[Byte] = {
    val arr = Array.fill[Byte](bitLen / 8)(0)
    rnd.nextBytes(arr)
    arr
  }

  /** Секретный ключ симметричного шифра. Сгенерить новый можно через generateSecretKey(). */
  // TODO В будущем следует придумать ротацию секретных ключей, чтобы генерились и ротировались во времени.
  private val SECRET_KEY: Array[Byte] = {
    Array[Byte](-22, 52, -78, -47, -46, 44, -3, 116, -8, -2, -96, -98, 48, 102, -117, -43,
                -59, -23, 75, 59, -101, 21, -26, 51, -102, -76, 22, 43, -94, -43, 111, 51)
  }

  // TODO следует свалить с дефолтовых алгоритмов (AES) на какую-нибудь маргинальщину из bouncy-castle.
  val CIPHER_SPEC = "AES/CBC/PKCS7Padding"
  val SECRET_KEY_ALGO = "AES"

  def encryptPrintable(str2enc: String): String = {
    try {
      val cipher = Cipher.getInstance(CIPHER_SPEC)
      val secretKey = new SecretKeySpec(SECRET_KEY, SECRET_KEY_ALGO)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val encryptedString = Base64.encodeBase64String(cipher.doFinal(str2enc.getBytes))
      encryptedString;
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Unable to encrypt string: " + str2enc, ex)
    }
  }

  def decryptPrintable(str2dec: String): String = {
    try {
      val cipher = Cipher.getInstance(CIPHER_SPEC)
      val secretKey = new SecretKeySpec(SECRET_KEY, SECRET_KEY_ALGO)
      cipher.init(Cipher.DECRYPT_MODE, secretKey)
      val decryptedString = new String(cipher.doFinal(Base64.decodeBase64(str2dec)))
      decryptedString
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Unable to decrypt string: " + str2dec, ex)
    }
  }

}
