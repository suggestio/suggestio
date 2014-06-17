package util.captcha

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import util.PlayLazyMacroLogsImpl

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
object CaptchaUtil {


}


object CipherUtil {

  // TODO В будущем следует придумать ротацию секретных ключей, чтобы генерились и ротировались во времени.
  // TODO Нужно расширить диапазон байтов ключа за пределы ASCII-таблицы.
  private val SECRET_KEY: Array[Byte] = "OnfoHecDuwavJuv3".getBytes

  // TODO следует свалить с дефолтовых алгоритмов на какой-нибудь serpent или blowfish из bouncy-castle
  val CIPHER_SPEC = "AES/CBC/PKCS5Padding"
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