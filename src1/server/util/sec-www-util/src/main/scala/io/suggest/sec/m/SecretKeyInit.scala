package io.suggest.sec.m

import java.security.SecureRandom

import io.suggest.util.logs.MacroLogsImplLazy
import play.api.{Configuration, Environment, Mode}
import japgolly.univeq._
import javax.inject.Inject

import scala.annotation.tailrec
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 19:49
 * Description: Доставатель секретных ключей из конфига. Если ключа в конфиге нет,
 * то будет предложено значение.
 */
trait SecretKeyInit {

  protected[m] def setSignSecret(secretKey: String): Unit

  def CONF_KEY: String

}


class SecretKeyInitializer @Inject() (
                                       configuration          : Configuration,
                                       env                    : Environment,
                                     )
  extends MacroLogsImplLazy
{

  def getRandomSecret: String = {
    val _rnd = new Random(new SecureRandom())
    val len = 64
    val sb = new StringBuilder(len)
    // Избегаем двойной ковычи в ключе, дабы не нарываться на проблемы при копипасте ключа в конфиг.
    @tailrec def nextPrintableCharNonQuote: Char = {
      val next = _rnd.nextPrintableChar()
      if (next ==* '"' || next ==* '\\')
        nextPrintableCharNonQuote
      else
        next
    }
    for (_ <- 1 to len) {
      sb append nextPrintableCharNonQuote
    }
    sb.toString()
  }


  /** Действия в случае отсутсвия секрета в конфиге. */
  def secretMissingFor(confKey: String): String = {
    val randomSecret = getRandomSecret
    val useRandomIfMissing = env.mode == Mode.Prod

    val warnMsg = s"Secret key '$confKey' not found found in your application.conf!"

    if (useRandomIfMissing) {
      val errorMsg = warnMsg
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

  def doInitOne(cls: SecretKeyInit): Unit = {
    val confKey = cls.CONF_KEY
    val secretKey = configuration
      .getOptional[String]( confKey )
      .getOrElse( secretMissingFor(confKey) )
    cls.setSignSecret( secretKey )
  }

  def initAll(classesForInit: SecretKeyInit*): Unit = {
    classesForInit.foreach( doInitOne )
  }

  def resetAll(classesForInit: SecretKeyInit*): Unit = {
    for (m <- classesForInit) {
      m.setSignSecret(null)
    }
  }

}
