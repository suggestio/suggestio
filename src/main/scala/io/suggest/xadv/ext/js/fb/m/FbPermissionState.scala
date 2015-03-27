package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.MaybeFromJsonT

import scala.scalajs.js.{Dictionary, WrappedDictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 16:13
 * Description: Модель состояния одного пермишшена.
 * @see [[https://developers.facebook.com/docs/facebook-login/permissions/v2.3#checking]]
 */
object FbPermissionState extends MaybeFromJsonT {

  override type T = FbPermissionState

  def maybeFromJson(raw: Any): Option[T] = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    for {
      permRaw     <- d.get("permission").map(_.toString)
      perm        <- FbPermissions.maybeWithName(permRaw)
      statusRaw   <- d.get("status").map(_.toString)
      status      <- FbPermissionStatuses.maybeWithName(statusRaw)
    } yield {
      FbPermissionState(
        permission = perm,
        status     = status
      )
    }
  }

}

/**
 * Состояние одного пермишшена.
 * @param permission Пермишшен приложения.
 * @param status Состояние пермишшена у юзера.
 */
case class FbPermissionState(
  permission  : FbPermission,
  status      : FbPermissionStatus
)
