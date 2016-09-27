package io.suggest.model.es

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:37
  * Description: Модель для приходящих извне строковых id, используемых в elasticsearch.
  * Эти id желательно проверять, матчить и т.д. перед отправкой в ES.
  * Поэтому, используется такая вот модель.
  */
object MEsId {

  /** Регэксп для проверки валидности id. */
  val uuidB64Re = "[_a-zA-Z0-9-]{19,25}".r

  /** Поддержка биндинга из/в qs. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MEsId] = {
    new QueryStringBindableImpl[MEsId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsId]] = {
        for (esIdE <- strB.bind(key, params)) yield {
          esIdE.right.flatMap { esId =>
            if ( uuidB64Re.pattern.matcher(esId).matches() ) {
              Right( MEsId(esId) )
            } else {
              Left( "e.invalid_id" )
            }
          }
        }
      }

      override def unbind(key: String, value: MEsId): String = {
        strB.unbind(key, value.id)
      }
    }
  }

  // TODO Переместить сюда же маппинг esIdM из FormUtil.

  import scala.language.implicitConversions

  implicit def esId2string(esId: MEsId): String = esId.id
  implicit def string2esId(id: String): MEsId   = MEsId(id)

}


/**
  * Инстанс модели.
  * @param id Строковой id.
  */
case class MEsId(id: String)
