package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 15:30
 * Description: Модели для взаимодействия с vk api методом wall.post.
 */
case class VkWallPostArgs(
  ownerId     : UserId_t,
  message     : Option[String],
  attachments : Seq[String]
) extends IToJsonDict {
  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any](
      "owner_id" -> ownerId
    )
    if (message.nonEmpty)
      d.update("message", message.get)
    if (attachments.nonEmpty)
      d.update("attachments", attachments.mkString(","))
    d
  }
}


object VkWallPostResult extends FromJsonT {
  override type T = VkWallPostResult

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    VkWallPostResult(
      error = d.get("error")
    )
  }
}

case class VkWallPostResult(error: Option[Any])
