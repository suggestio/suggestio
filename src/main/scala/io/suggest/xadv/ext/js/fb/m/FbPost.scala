package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 14:19
 * Description: Модель для представления поста для ввода-вывода.
 */
case class FbPost(
  picture   : Option[String],
  message   : Option[String],
  link      : Option[String],
  name      : Option[String],
  descr     : Option[String],
  accessToken: Option[String]   = None,
  caption   : Option[String]    = Some("suggest.io"),
  privacy   : Option[FbPrivacy] = Some(FbPrivacy())
) extends IToJsonDict with IFbAccessTokenOpt {

  override def toJson: Dictionary[Any] = {
    val d = Dictionary.empty[Any]
    if (picture.nonEmpty)
      d.update("picture", picture.get)
    if (message.nonEmpty)
      d.update("message", message.get)
    if (link.nonEmpty)
      d.update("link", link.get)
    if (name.nonEmpty)
      d.update("name", name.get)
    if (descr.nonEmpty)
      d.update("description", descr.get)
    if (accessToken.nonEmpty)
      d.update("access_token", accessToken.get)
    if (caption.nonEmpty)
      d.update("caption", caption.get)
    if (privacy.nonEmpty)
      d.update("privacy", privacy.get.toJson)
    d
  }
}


/** Модель содержимого поля privacy, описывающего настройки приватности. */
case class FbPrivacy(v: String = "EVERYONE") extends IToJsonDict {
  override def toJson = Dictionary[Any](
    "value" -> v
  )
}


object FbPostResult extends FromJsonT {
  override type T = FbPostResult

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbPostResult(
      // Обычно id созданного поста приходит
      postId = d.get("id")
        .orElse { d.get("post_id") }    // TODO После обновления api это поле ещё где-нить встречается?
        .map(_.toString),
      error  = d.get("error")
    )
  }
}
case class FbPostResult(postId: Option[String], error: Option[Any])
