package util.qsb

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import play.api.mvc.QueryStringBindable
import play.core.parsers.FormUrlEncodedParser
import util.PlayMacroLogsImpl
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 21:38
 * Description: QSB для подписывания qs с помощью HMAC.
 */
object QsbSigner {

  val ALGO_DFLT = configuration.getString("qsb.signer.mac.algo.dflt") getOrElse "HmacSHA1"

  val SIG_INVALID_MSG = configuration.getString("qsb.signer.signature.invalid.msg") getOrElse "Invalid signature."

}

import QsbSigner._


/**
 * QSB для подписывания параметров, передаваемых в qs в рамках указанного ключа.
 * @param secretKey Ключ подписи. Например "item". Лучше вместе с точкой в конце, если проверяются под-параметры.
 * @param signKeyName Имя qs-ключа с подписью.
 * @param strB QSB-биндер для строк.
 */
class QsbSigner(secretKey: String, signKeyName: String, algo: String = QsbSigner.ALGO_DFLT)
               (implicit strB: QueryStringBindable[String])
extends QueryStringBindable[Map[String, Seq[String]]]
with PlayMacroLogsImpl
{
  import LOGGER._

  /** Итератор по карте параметров, который возвращает только подписанные параметры. */
  def onlyParamsForKey(key: String, params: Map[String, Seq[String]]): Iterator[(String, Seq[String])] = {
    params
      .iterator
      .filter { case (k, _) => k.startsWith(key) && k != signKeyName }
  }

  def mkMac = {
    val mac = Mac.getInstance(algo)
    mac.init(new SecretKeySpec(secretKey.getBytes, algo))
    mac
  }

  /** Посчитать подпись для указанной карты параметров. */
  def mkSignForMap(key: String, params: Map[String, Seq[String]]): String = {
    val params2 = onlyParamsForKey(key, params)
    mkSignForMap(params2)
  }
  def mkSignForMap(params: TraversableOnce[(String, Seq[String])]): String = {
    val mac = mkMac
    // Объявляем вне цикла используемые разделители ключей, значений и их пар:
    val kvDelim = "=".getBytes
    val kkDelim = "&".getBytes
    // Нужно отсортировать все данные:
    params.toIterator
      .map { case (k, vs) => k -> vs.sorted}
      .toSeq
      .sortBy(_._1)
      // Последовательно закидываем все данные в mac-аккамулятор:
      .foreach { case (k, vs) =>
        vs.foreach { v =>
          // Имитируем ввода данных типа "k=aasdasd&b=123&..."
          mac.update(k.getBytes)
          mac.update(kvDelim)
          mac.update(v.getBytes)
          mac.update(kkDelim)
        }
      }
    // Переводим результат в строку.
    Hex.encodeHexString(mac.doFinal())
  }

  /**
   * Проверить подпись на параметрах, имена которых начинаются на key.
   * @param key Префикс ключей, по которым идёт проверка подписи.
   * @param params Карта qs.
   * @return Результаты проверки подписи, в частности карта параметров, которые прошли проверку подписи.
   *         None - если подпись не найдена.
   */
  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Map[String, Seq[String]]]] = {
    strB.bind(signKeyName, params).map { maybeSignature =>
      maybeSignature.right.flatMap { signature =>
        val pfk = onlyParamsForKey(key, params).toStream
        val realSignature = mkSignForMap(pfk)
        if (realSignature equalsIgnoreCase signature) {
          // Всё ок, собираем подходящие результаты в кучу.
          Right(pfk.toMap)
        } else {
          warn(s"Invalid qsb signature for key '$key': expected=$signature real=$realSignature\n params = $params")
          Left(SIG_INVALID_MSG)
        }
      }
    }
  }

  def signedOrNone(key: String, params: Map[String, Seq[String]]): Option[Map[String, Seq[String]]] = {
    bind(key, params)
      .flatMap {
        case Right(result) => Some(result)
        case left => None
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
    value + (signKeyName -> Seq(s))
  }

  /**
   * Подпись для готовой qs-строки.
   * @param qsStr Строка qs после предшествующих unbind'ов.
   * @return Подписанная строка qs.
   */
  def mkSigned(key: String, qsStr: String): String = {
    val paramsMap = FormUrlEncodedParser.parse(qsStr)
    val signature = mkSignForMap(key, paramsMap)
    val sb = new StringBuilder(qsStr)
    if (!qsStr.isEmpty) {
      sb.append('&')
    }
    sb.append(signKeyName).append('=').append(signature)
      .toString()
  }

  /**
   * Подписать и сериализовать в qs-строку значения, переданные в виде карты параметров.
   * @param key Префикс ключей подписываемых данных в карте.
   * @param value Карта с данными, которые подлежат подписыванию.
   * @return Строка qs с сериализованной картой параметров и параметром с подписью.
   */
  override def unbind(key: String, value: Map[String, Seq[String]]): String = {
    paramsSigned(key, value)
      .iterator
      .flatMap { case (k, vs) => vs.map(k -> _) }
      .map { case (k, v) => strB.unbind(k, v) }
      .mkString("&")
  }

}
