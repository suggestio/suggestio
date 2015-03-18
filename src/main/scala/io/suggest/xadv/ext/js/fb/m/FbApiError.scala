package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.FromJsonT
import org.scalajs.dom

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 17:36
 * Description: Модель представления ошибки API.
 * Ошибка в указанном формате обычно приходит в JSON-ответе в поле "error.
 */

object FbApiError extends FromJsonT {
  override type T = FbApiError

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]: WrappedDictionary[Any]
    FbApiError(
      message = d.get("message").fold("")(_.toString),
      eType   = d.get("type").map(_.toString),
      code    = d.get("code").flatMap { v =>
        try {
          Some(v.asInstanceOf[Int])
        } catch {
          case ex: Throwable =>
            dom.console.warn("Cannot parse error code from '%s': %s %s", v, ex.getClass.getName, ex.getMessage)
            None
        }
      }
    )
  }
}

case class FbApiError(message: String, eType: Option[String], code: Option[Int])

