package util.secure

import java.security.SecureRandom
import play.api.Play.{current, configuration, isProd}
import util.PlayMacroLogsI

import scala.annotation.tailrec
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 19:49
 * Description: Доставатель секретных ключей из конфига. Если ключа в конфиге нет,
 * то будет предложено значение.
 */
trait SecretGetter extends PlayMacroLogsI {
  
  def confKey: String
  
  def getRandom = new Random(new SecureRandom())

  /** Кажется, этот параметр надо переименовать. Т.к. он по сути работает задом наперед. */
  def useRandomIfMissing: Boolean = isProd

  def secretKeyLen: Int = 64

  def getRandomSecret: String = {
    val _rnd = getRandom
    val len = secretKeyLen
    val sb = new StringBuilder(len)
    // Избегаем двойной ковычи в ключе, дабы не нарываться на проблемы при копипасте ключа в конфиг.
    @tailrec def nextPrintableCharNonQuote: Char = {
      val next = _rnd.nextPrintableChar()
      if (next == '"' || next == '\\')
        nextPrintableCharNonQuote
      else
        next
    }
    for(i <- 1 to len) {
      sb append nextPrintableCharNonQuote
    }
    sb.toString()
  }

  def warnMsg: String = s"Secret key '$confKey' not found found in your application.conf!"
  def errorMsg: String = warnMsg

  /** Действия в случае отсутсвия секрета в конфиге. */
  def secretMissing: String = {
    val randomSecret = getRandomSecret
    if (useRandomIfMissing) {
      // В продакшене без ключа нельзя. Генерить его и в логи писать его тоже писать не стоит наверное.
      throw new IllegalStateException(
        s"""$errorMsg
             |Cannot continue without secret key. Please add in your application.conf:
             |$confKey = "$randomSecret"
             |Use same value on ALL PRODUCTION NODES!""".stripMargin
      )
    } else {
      val result = randomSecret
      LOGGER.warn(
        s"""$warnMsg
             |Please add this into application.conf:
             |$confKey = "$result" """.stripMargin
      )
      result
    }
  }

  def getSecret = configuration.getString(confKey)

  /**
   * Используя имеющиеся данные, попытаться извлечь из конфига значение секретного ключа
   * и принять последующие решения.
   * @return Значение секретного ключа строкой либо экзепшен.
   */
  def apply(): String = getSecret getOrElse secretMissing

}
