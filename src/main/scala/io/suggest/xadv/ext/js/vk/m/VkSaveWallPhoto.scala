package io.suggest.xadv.ext.js.vk.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js
import scala.scalajs.js.{JSON, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 14:04
 * Description: Модели параметров и результатов вызова vk api метода photos.saveWallPhoto.
 */

object VkSaveWallPhotoArgs {
  def apply(userId: UserId_t, raw: String): VkSaveWallPhotoArgs = {
    val d = JSON.parse(raw)
      .asInstanceOf[js.Dictionary[js.Any]]
    apply(userId = userId, args = d)
  }
}

case class VkSaveWallPhotoArgs(
  userId  : UserId_t,
  args    : js.Dictionary[js.Any]
) extends IToJsonDict {
  override def toJson: Dictionary[js.Any] = {
    args.update("user_id", userId)
    args
  }
}



object VkSaveWallPhotoResult extends FromJsonT {
  override type T = VkSaveWallPhotoResult

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Any]]
    val resp = d("response").asInstanceOf[js.Array[js.Dictionary[js.Any]]]
    VkSaveWallPhotoResult(
      id = resp(0)("id").toString
    )
  }
}

case class VkSaveWallPhotoResult(id: String)
