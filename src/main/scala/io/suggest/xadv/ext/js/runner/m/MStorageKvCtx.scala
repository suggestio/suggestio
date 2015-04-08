package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MStorageKvCtx._
import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.15 10:41
 * Description: Аргументы для операций чтения/записи в хранилище браузера.
 */

object MStorageKvCtx extends FromJsonT {
  override type T = MStorageKvCtx

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    MStorageKvCtx(
      key     = d(KEY_FN).toString,
      value   = d.get(VALUE_FN).map(_.toString)
    )
  }
}

case class MStorageKvCtx(
  key   : String,
  value : Option[String]
) extends IToJsonDict {

  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any](
      KEY_FN -> key
    )
    if (value.isDefined)
      d.update(VALUE_FN, value.get)
    d
  }
}
