package models.adv

import play.api.mvc.QueryStringBindable
import util.{PlayMacroLogsDyn, FormUtil}

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.01.15 14:59
 * Description: Модель для представления информации по таргетам.
 * Передаются в веб-сокет через qsb. А оттуда попадает в ext-adv-акторы.
 */

object MExtTargetInfo extends ExtTargetInfoParsers with PlayMacroLogsDyn {

  /** Разделитель полей в сериализованной форме. */
  def DELIMITER = ','

  /** QueryStringBindable для экземпляров текущей модели. */
  implicit def qsb(implicit strB: QueryStringBindable[String]) = {
    new QueryStringBindable[MExtTargetInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MExtTargetInfo]] = {
        for {
          ser <- strB.bind(key, params)
        } yield {
          ser.right.flatMap { s =>
            val pr = parse(targetInfoParserP, s)
            if (pr.successful) {
              Right(pr.get)
            } else {
              // Крайне невероятный сценарий, что тут будет ошибка. Обычно это qs-значение покрыто hmac.
              LOGGER.warn(s"qsb(): Cannot parse $key = $s\n  params = $params")
              Left("Cannot parse return mode.")
            }
          }
        }
      }

      override def unbind(key: String, value: MExtTargetInfo): String = {
        new StringBuilder(32, key)
          .append('=')
          .append(value.targetId)
          .append(DELIMITER)
          .append(value.returnTo.strId)
          .toString()
      }
    }
  }

}


/** Парсеры для чтения сериализованных info'шек из значений qs. */
trait ExtTargetInfoParsers extends JavaTokenParsers {

  /** Парсер targetId. */
  def targetIdP: Parser[String] = {
    FormUtil.uuidB64Re
  }

  /** Парсер разделителя полей. */
  def delimP: Parser[_] = {
    MExtTargetInfo.DELIMITER
  }

  /** Парсер сериализованного поля возврата. */
  def returnToP: Parser[MExtReturn] = {
    // Параметры длины вынесены сюда, чтобы гарантировано подавить re-evaluation при множественных вызовах этого парсера.
    val maxLen = MExtReturns.strIdLenMax
    val minLen = MExtReturns.strIdLenMin
    s"\\w{$minLen,$maxLen}".r ^^ {
      raw => MExtReturns.withName(raw.trim)
    }
  }

  /** Финальный парсер, генерящий экземпляры модели. */
  def targetInfoParserP: Parser[MExtTargetInfo] = {
    ((targetIdP <~ delimP) ~ returnToP) ^^ {
      case targetId ~ returnTo =>
        MExtTargetInfo(targetId = targetId, returnTo = returnTo)
    }
  }

}


/** Общий интерфейс экземпляров target info. Пока не нужен. */
sealed trait IReturnTo {
  def returnTo: MExtReturn
}


/**
 * Экземпляр одной информации о цели. Обычно генерится силами маппинга формы.
 * @param targetId id цели.
 * @param returnTo Режим возврата юзера на s.io.
 */
case class MExtTargetInfo(
  targetId      : String,
  returnTo      : MExtReturn
) extends IReturnTo


/**
 * Контейнер для рантаймовых данных в голове у системы.
 * @param target Экземпляр цели.
 * @param returnTo Режим возврата юзера на s.io.
 */
case class MExtTargetInfoFull(
  target        : MExtTarget,
  returnTo      : MExtReturn
) extends IReturnTo

