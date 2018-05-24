package io.suggest.es.model

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:37
  * Description: Модель для приходящих извне строковых id, используемых в elasticsearch.
  * Эти id желательно проверять, матчить и т.д. перед отправкой в ES.
  * Поэтому, используется такая вот модель.
  */

object MEsUuId {

  def charsAllowedRe = "[_a-zA-Z0-9-]"

  /** Регэксп для проверки валидности id. */
  val uuidB64Re = (charsAllowedRe + "{19,25}").r

  /** Регэксп для парсинга uuid, закодированного в base64, допускающего необычно-большую длину.
    * Удлиненние id может быть у маячков.
    */
  val uuidB64Re60 = (MEsUuId.charsAllowedRe + "{19,60}").r

  /** Проверить id по допустимым символам. У uuid и any id алфавиты одинаковые, только длина разная. */
  def isEsIdValid(id: String): Boolean = {
    uuidB64Re60.pattern.matcher(id).matches()
  }


  /** Пропарсить строку и завернуть в [[MEsUuId]] если всё ок.
    * @param value исходная строка с id.
    * @return Right с инстансом [[MEsUuId]].
    *         Left с кодом ошибки.
    */
  def fromStringEith(value: String): Either[String, MEsUuId] = {
    if ( uuidB64Re.pattern.matcher(value).matches() ) {
      Right( MEsUuId(value) )
    } else {
      Left( "e.invalid_id" )
    }
  }


  /** Поддержка play-json. */
  implicit def mEsUuIdFormat: Format[MEsUuId] = {
    implicitly[Format[String]]
      .inmap[MEsUuId]( apply, _.id )
  }


  // TODO Переместить сюда же маппинг esIdM из FormUtil.

  import scala.language.implicitConversions

  implicit def esId2string(esId: MEsUuId): String = esId.id
  implicit def string2esId(id: String): MEsUuId   = MEsUuId(id)

  implicit def esIdOpt2strOpt(esIdOpt: Option[MEsUuId]): Option[String] = esIdOpt.map(esId2string)

  implicit def univEq: UnivEq[MEsUuId] = UnivEq.derive


  object Implicits {

    implicit class StrOptExtOps( val strOpt: Option[String] ) extends AnyVal {

      def toEsUuIdOpt: Option[MEsUuId] = {
        strOpt.map { MEsUuId.apply }
      }

    }

  }

}


/**
  * Инстанс модели.
  * @param id Строковой id.
  */
case class MEsUuId(id: String) {
  override def toString = id
}
