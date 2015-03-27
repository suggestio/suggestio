package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.FromJsonT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 14:27
 * Description: Модель объекта authResponse, который фигурирует в результатах логина и связанных с логинам методами.
 *
 * JSON-формат authResponse таков: {
 *   accessToken: "CAANwC6...",
 *   expiresIn: 7127
 *   grantedScopes: "public_profile,manage_pages,publish_actions,user_photos",
 *   signedRequest: "smhuX....",
 *   userID: "5646456235636346545"
 * }
 * @see [[https://developers.facebook.com/docs/reference/javascript/FB.getLoginStatus#response_and_session_objects]]
 */

object FbAuthResponse extends FromJsonT {

  override type T = FbAuthResponse

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbAuthResponse(
      accessToken   = d.get("accessToken")
        .map(_.toString),
      userId        = d.get("userID")
        .map(_.toString),
      grantedScopesRaw = d.get("grantedScopes")
        .map(_.toString)
    )
  }

}

case class FbAuthResponse(
  accessToken       : Option[String],
  userId            : Option[String],
  grantedScopesRaw  : Option[String]
) {

  /** Распарсенное значение поля grantedScopesRaw. */
  lazy val grantedScopes: Seq[FbPermission] = {
    grantedScopesRaw match {
      case Some(gsRaw) =>
        gsRaw.split(",")
          .iterator
          .flatMap { FbPermissions.maybeWithName }
          .toSeq

      case None =>
        Seq.empty
    }
  }
  
}
