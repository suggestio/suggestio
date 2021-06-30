package io.suggest.sec

import javax.crypto.Mac
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import org.apache.commons.codec.binary.Hex
import play.api.mvc.QueryStringBindable
import play.core.parsers.FormUrlEncodedParser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 21:38
 * Description: QSB для подписывания qs с помощью HMAC.
 */
object QsbSigner {

  def SIG_INVALID_MSG = "Invalid signature."

}

import io.suggest.sec.QsbSigner._


/**
 * QSB для подписывания параметров, передаваемых в qs в рамках указанного ключа.
 * @param secretKey Ключ подписи. Например "item". Лучше вместе с точкой в конце, если проверяются под-параметры.
 * @param signKeyName Имя qs-ключа с подписью.
 */
class QsbSigner(secretKey: String, signKeyName: String)
  extends AbstractQueryStringBindable[Map[String, Seq[String]]]
  with MacroLogsImpl
{

  if (secretKey == null)
    throw new IllegalStateException(getClass.getSimpleName + " not initialized: missing secret key")

  /** Итератор по карте параметров, который возвращает только подписанные параметры. */
  def onlyParamsForKey(key: String, params: Iterable[(String, Seq[String])]): Iterator[(String, Seq[String])] = {
    params
      .iterator
      .filter { case (k, _) => k.startsWith(key) && k != signKeyName }
  }


  def mkMac(): Mac = {
    HmacUtil.mkMac( HmacAlgos.HMAC_SHA1, secretKey )
  }


  /** Посчитать подпись для указанной карты параметров. */
  def mkSignForMap(key: String, params: Iterable[(String, Seq[String])]): String = {
    val params2 = onlyParamsForKey(key, params)
    mkSignForMap(params2)
  }
  def mkSignForMap(params: IterableOnce[(String, Seq[String])]): String = {
    val mac = mkMac()
    // Объявляем вне цикла используемые разделители ключей, значений и их пар:
    val kvDelim = "=".getBytes
    val kkDelim = "&".getBytes

    // Нужно отсортировать все данные:
    for {
      (k, vs) <- {
        params
          .iterator
          .map { case (a, avs) =>
            a -> avs.sorted
          }
          .toSeq
          .sortBy(_._1)
      }

      v <- vs
    } {
      // Последовательно закидываем все данные в mac-аккамулятор:
      // Имитируем ввода данных типа "k=aasdasd&b=123&..."
      mac.update(k.getBytes)
      mac.update(kvDelim)
      mac.update(v.getBytes)
      mac.update(kkDelim)
    }

    // Переводим результат в строку.
    Hex.encodeHexString(mac.doFinal())
  }

  private def stringB = implicitly[QueryStringBindable[String]]

  /**
   * Проверить подпись на параметрах, имена которых начинаются на key.
   * @param key Префикс ключей, по которым идёт проверка подписи.
   * @param params Карта qs.
   * @return Результаты проверки подписи, в частности карта параметров, которые прошли проверку подписи.
   *         None - если подпись не найдена.
   */
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Map[String, Seq[String]]]] = {
    for (maybeSignature <- stringB.bind(signKeyName, params)) yield {
      maybeSignature.flatMap { signature =>
        val pfk = onlyParamsForKey(key, params).toSeq
        LOGGER.trace(s"bind($key): Params:\n All: ${params.mkString(" & ")};\n onlyForKey: ${pfk.mkString(" & ")}")

        val realSignature = mkSignForMap(pfk)
        if (realSignature equalsIgnoreCase signature) {
          // Всё ок, собираем подходящие результаты в кучу.
          Right(pfk.toMap)
        } else {
          LOGGER.warn(s"Invalid QSB signature for key '$key': expected=$signature real=$realSignature\n params = $params")
          Left(SIG_INVALID_MSG)
        }
      }
    }
  }


  def signedOrNone(key: String, params: Map[String, Seq[String]]): Option[Map[String, Seq[String]]] = {
    bind(key, params)
      .flatMap {
        _.toOption
      }
  }


  /**
   * Подписать qs-карту параметров, создав новую карту параметров.
   * @param key Ключ подписываемых данных в карте.
   * @param value Исходная карта qs-параметров.
   * @return Новая карта qs-параметров, содержащая в себе кортеж с подписью.
   */
  def paramsSigned(key: String, value: Map[String, Seq[String]]): Map[String, Seq[String]] = {
    val s = mkSignForMap(key, value)
    value + (signKeyName -> (s :: Nil))
  }


  /**
   * Подпись для готовой qs-строки.
   * @param qsStr Строка qs после предшествующих unbind'ов.
   * @return Подписанная строка qs.
   */
  def mkSigned(key: String, qsStr: String): String = {
    val paramsMap = FormUrlEncodedParser
      .parse(qsStr)
      // play-2.5: почему-то этот метод для пустой строки возвращает "" -> ArrayBuffer().
      .view
      .filterKeys(_.nonEmpty)

    val signature = mkSignForMap(key, paramsMap.toMap)
    LOGGER.trace(s"mkSigned($key):\n qsStr = $qsStr\n paramsMap = ${paramsMap.mkString(" & ")}\n signature = $signature")

    val sb = new StringBuilder(qsStr)
    if (!qsStr.isEmpty)
      sb.append('&')
    sb.append(signKeyName)
      .append('=')
      .append(signature)
      .toString()
  }


  /**
   * Подписать и сериализовать в qs-строку значения, переданные в виде карты параметров.
   * @param key Префикс ключей подписываемых данных в карте.
   * @param value Карта с данными, которые подлежат подписыванию.
   * @return Строка qs с сериализованной картой параметров и параметром с подписью.
   */
  override def unbind(key: String, value: Map[String, Seq[String]]): String = {
    val _stringB = stringB
    (for {
      (k, vs) <- paramsSigned( key, value ).iterator
      v <- vs
    } yield {
      _stringB.unbind( k, v )
    })
      .mkString("&")
  }


  /** Имя класса рендерится в логах без secret key, даже если в будущем класс станет как case class. */
  override def toString: String = {
    s"${getClass.getSimpleName}($signKeyName)"
  }

}
