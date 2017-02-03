package io.suggest.es.model

import io.suggest.model.play.psb.PathBindableImpl
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:37
  * Description: Модель для приходящих извне строковых id, используемых в elasticsearch.
  * Эти id желательно проверять, матчить и т.д. перед отправкой в ES.
  * Поэтому, используется такая вот модель.
  *
  * До 17.10.2016 модель называлась MEsId, но это не отражала её сильную завязанность на UUID.
  */
object MEsUuId {

  def charsAllowedRe = "[_a-zA-Z0-9-]"

  /** Регэксп для проверки валидности id. */
  val uuidB64Re = (charsAllowedRe + "{19,25}").r


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

  /** Поддержка биндинга из/в qs. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MEsUuId] = {
    new QueryStringBindableImpl[MEsUuId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsUuId]] = {
        for (esIdE <- strB.bind(key, params)) yield {
          esIdE.right
            .flatMap(fromStringEith)
        }
      }

      override def unbind(key: String, value: MEsUuId): String = {
        strB.unbind(key, value.id)
      }
    }
  }


  /** PathBinadable для биндинга значения id прямо из URL path. */
  implicit def psb(implicit strB: PathBindable[String]): PathBindable[MEsUuId] = {
    new PathBindableImpl[MEsUuId] {
      override def bind(key: String, value: String): Either[String, MEsUuId] = {
        fromStringEith(value)
      }
      override def unbind(key: String, value: MEsUuId): String = {
        value
      }
    }
  }


  // TODO Переместить сюда же маппинг esIdM из FormUtil.

  import scala.language.implicitConversions

  implicit def esId2string(esId: MEsUuId): String = esId.id
  implicit def string2esId(id: String): MEsUuId   = MEsUuId(id)


}


/**
  * Инстанс модели.
  * @param id Строковой id.
  */
case class MEsUuId(id: String) {
  override def toString = id
}
