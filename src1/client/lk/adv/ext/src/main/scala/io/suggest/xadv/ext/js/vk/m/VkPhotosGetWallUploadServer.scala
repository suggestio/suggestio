package io.suggest.xadv.ext.js.vk.m

import io.suggest.sjs.common.model.{IToJsonDict, FromJsonT}

import scala.scalajs.js
import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 18:53
 * Description:
 */

case class VkPhotosGetWallUploadServerArgs(userId: UserId_t) extends IToJsonDict {
  override def toJson = Dictionary[Any](
    "user_id" -> userId
  )
}


object VkPhotosGetWallUploadServerResult extends FromJsonT {
  override type T = VkPhotosGetWallUploadServerResult
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[js.Any]] : WrappedDictionary[js.Any]
    val resp = d("response").asInstanceOf[Dictionary[Any]]
    VkPhotosGetWallUploadServerResult(
      uploadUrl = resp("upload_url").toString
    )
  }
}

case class VkPhotosGetWallUploadServerResult(
  uploadUrl: String
)
