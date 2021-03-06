package io.suggest.es.model

import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:37
  * Description: Модель для приходящих извне строковых id, используемых в elasticsearch.
  * Эти id желательно проверять, матчить и т.д. перед отправкой в ES.
  * Поэтому, используется такая вот модель.
  */

object MEsUuId {

  def charsAllowedRe = "[._a-zA-Z0-9-]"

  private final def MIN_STR_ID_LEN = 11

  /** Регэксп для проверки валидности id.
    * min=11 т.к. 404-node содержит id вида {{{ .___404___.ru }}}
    */
  val uuidB64Re = s"$charsAllowedRe{$MIN_STR_ID_LEN,25}".r


  /** Регэксп для парсинга uuid, закодированного в base64, допускающего необычно-большую длину.
    * Удлиненние id может быть у маячков.
    */
  val uuidB64Re60 = s"$charsAllowedRe{$MIN_STR_ID_LEN,60}".r

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
    if ( uuidB64Re60.pattern.matcher(value).matches() ) {
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

  @inline implicit def univEq: UnivEq[MEsUuId] = UnivEq.derive


  object Implicits {

    implicit class StrOptExtOps( val strOpt: Option[String] ) extends AnyVal {

      def toEsUuIdOpt: Option[MEsUuId] =
        strOpt.map { MEsUuId.apply }

    }

  }


  implicit class EsUuIdOptExtOps( val esIdOpt: Option[MEsUuId] ) extends AnyVal {

    def toStringOpt: Option[String] =
      esIdOpt.map( _.id )

    def containsStr(str: String): Boolean =
      esIdOpt.exists(_.id ==* str)

  }

}


/**
  * Инстанс модели.
  * @param id Строковой id.
  */
final case class MEsUuId(id: String) extends IId[String] {
  override def toString = id
}
