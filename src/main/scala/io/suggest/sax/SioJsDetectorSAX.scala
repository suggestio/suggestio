package io.suggest.sax

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import io.suggest.util.{LogsImpl, UrlUtil}
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 17:30
 * Description: SAX-handler для обнаружения тега suggest.js на странице.
 */

object SioJsDetectorSAX extends Serializable {

  // Версии всякие.
  object SioJsVersion extends Enumeration {
    type SioJsVersion = Value
    val JSv1, JSv2 = Value
  }

  // TODO Следует отрабатывать автоматический выбор домена поиска.
  val reStr = "https?://(?:(?:www\\.)?suggest\\.io|localhost:9000)/(?:static/)?js/?(?:v2/([^/]+)/([a-zA-Z0-9]+))?"
  val srcReStr = "^\\s*" + reStr + "\\s*$"

  // Какие группы экстрактить из регэкспов. Тут две группы, они будут отражены на домен и qi_id.
  val groups = List(4, 5)

  val TAG_SCRIPT = "script"
  val ATTR_SRC   = "src"

  val SCRIPT_BODY_ACC_LEN_MAX = 2048
}


import SioJsDetectorSAX._
class SioJsDetectorSAX extends DefaultHandler with Serializable {

  val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  // Скриптов на странице бывает несколько (это ошибочно, но надо это тоже отрабатывать).
  protected var scriptInfoAcc : List[SioJsInfoT] = List()

  // Регэксп. Компилируем его в конструкторе класса, чтобы избежать ненужной передачи скомпиленного регэкспа.
  val srcRe = srcReStr.r
  val textRe = reStr.r

  protected var scriptBodyAccCharLen = 0

  // Мы внутри тега script? Если > 0, то да.
  protected var inScript = 0

  protected var scriptBodyAcc = List[String]()


  /**
   * Начало тега. Если это script то надо залезть внутрь тега и поглядеть.
   * @param uri
   * @param localName имя тега
   * @param qName
   * @param attributes аттрибуты тега
   */
  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
    super.startElement(uri, localName, qName, attributes)
    // Если это script
    if (localName == TAG_SCRIPT) {
      inScript = inScript + 1
      // Если src задан, то нужно его проанализировать
      val src = attributes.getValue(ATTR_SRC)
      if (src != null) {
        maybeMatches(srcRe, src)
      }
    }
  }


  /**
   * Если мы в скрипте, то надо собирать его текст в аккамулятор, чтобы потом отматчить
   * @param ch много букв
   * @param start начало последовательности символов
   * @param length длина
   */
  override def characters(ch: Array[Char], start: Int, length: Int) {
    super.characters(ch, start, length)

    if (inScript > 0) {
      val newAccLen = scriptBodyAccCharLen + length
      if (newAccLen <= SCRIPT_BODY_ACC_LEN_MAX) {
        scriptBodyAcc ::= new String(ch, start, length)
        scriptBodyAccCharLen = newAccLen
      }
    }
  }


  /**
   * Выход из тега. Если из script, то декрементить inScript
   * @param uri
   * @param localName имя тега
   * @param qName
   */
  override def endElement(uri: String, localName: String, qName: String) {
    super.endElement(uri, localName, qName)

    if (localName == TAG_SCRIPT) {
      inScript = inScript - 1
      // Не давать счетчику уходить в минус
      if (inScript < 0)
        inScript = 0

      // Если произошел полный выход из script-тега и аккамулятор не пуст, то надо поглядеть в акк и принять решение
      if (inScript == 0 && scriptBodyAccCharLen > 10) {
        // Проверить аккамулятор на наличие скрипта
        val scriptBody = scriptBodyAcc.reverse.mkString
        maybeMatches(textRe, scriptBody)
        // Очистить аккамулятор тела скрипта
        scriptBodyAcc = List()
        scriptBodyAccCharLen = 0
      }
    }
  }


  /**
   * Выдать списочек результатов.
   * @return
   */
  def getSioJsInfo = scriptInfoAcc.distinct


  /**
   * Проверить текст по отношению к регэкспу. Результат закинуть в аккамулятор.
   * @param re регэксп
   * @param text текст
   */
  protected def maybeMatches(re:Regex, text:String) {
    re findAllMatchIn text foreach { m =>
      scriptInfoAcc ::= SioJsInfo(m.subgroups)
    }
  }

}


import SioJsVersion._

trait SioJsInfoT {
  def version : SioJsVersion

  override def hashCode(): Int = 829 * version.hashCode()
}

object SioJsInfo {

  /**
   * Сгенерить нужный класс на основе распарсенных значений
   * @return
   */
  def apply(values : List[String]) : SioJsInfoT = {
    values match {
      case List(d, q) if d != null && q != null && d.length > 3 && q.length > 4 =>
        val dn = UrlUtil.normalizeHostname(d)
        SioJsV2(dn, q)

      case _ => SioJsV1
    }
  }

}


// Тут типа класс-сингтон в виде объекта, ибо такой класс не принимает параметров. Это ускорит сравнение при List.distinct()
object SioJsV1 extends SioJsInfoT {
  def version = JSv1
}

// v2-версия имеет параметры, и сравнение нужно делать аккуратнее.
case class SioJsV2(dkey:String, qi_id:String) extends SioJsInfoT {
  def version = JSv2

  /**
   * Целочисленный хеш объекта
   * @return
   */
  override def hashCode(): Int = super.hashCode() + dkey.hashCode + qi_id.hashCode

  /**
   * Сравнение объектов, ибо используется distinct.
   * @param obj объект, с которым происходит сравнение
   * @return
   */
  override def equals(obj: Any): Boolean = {
    super.equals(obj) || (obj match {
      case SioJsV2(_dkey, _qi_id) if dkey == _dkey && qi_id == _qi_id => true
      case _ => false
    })
  }
}
