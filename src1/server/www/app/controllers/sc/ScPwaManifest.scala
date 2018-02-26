package controllers.sc

import io.suggest.i18n.MsgCodes
import io.suggest.pick.MimeConst
import io.suggest.pwa.manifest.{MPwaDisplayModes, MWebManifest}
import io.suggest.sc.pwa.MPwaManifestQs
import models.im.MFavIcons
import play.api.libs.json.Json
import util.acl.IMaybeAuth

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:00
  * Description: Трейт с экшеном и сопутствующей обвязки манифеста веб-приложения.
  *
  * @see [[https://developer.mozilla.org/en-US/docs/Web/Manifest]]
  */
trait ScPwaManifest
  extends ScController
  with IMaybeAuth
{

  /** Экшен раздачи json-манифеста с описаловом вёб-приложения.
    *
    * @param qs Данные url qs.
    * @return 200 + JSON
    */
  def webAppManifest(qs: MPwaManifestQs) = maybeAuth(U.PersonNode).async { implicit request =>
    val manifest = MWebManifest(
      name      = MsgCodes.`Suggest.io`,
      startUrl  = "/",
      display   = Some( MPwaDisplayModes.Standalone ),
      icons     = MFavIcons.linkRelIcons.map(_.icon)
    )
    val respJson = Json.toJson( manifest )
    Ok(respJson)
      .as( MimeConst.WEB_APP_MANIFEST )
      // TODO Протюнить cache-control под реальную обстановку. 86400сек - это с потолка.
      .cacheControl(86400)
  }

  // application/manifest+json

}
