package controllers

import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.msc.ScJsState
import util.acl.MaybeAuth

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.2020 10:12
  * Description: Контроллер поддержки коротких ссылок.
  * Короткие ссылки для кодирования внутрь маячков EddyStone-URL, где макс.длина ссылки - это 17 байт.
  */
final class ShortUrls @Inject() (
                                  maybeAuth           : MaybeAuth,
                                  sioControllerApi    : SioControllerApi,
                                )
  extends MacroLogsImplLazy
{

  import sioControllerApi._


  /** Экшен перехвата короткой ссылки: определить адрес для редиректа на основе кода в ссылке.
    *
    * @param urlCode Код после тильды: a-zA-Z0-9 и другие URL-safe символы.
    * @return Редирект куда-либо.
    */
  def handleUrl(urlCode: String) = maybeAuth().async { implicit request =>
    // Изначально, id содержал короткий алиас для URL внутри EddyStone-маячка.
    // TODO Поискать маячки с urlCode в качестве id в ShortUrl-эдже.
    val call = routes.Sc.geoSite(
      a = ScJsState(
        // TODO Отредиректить в выдачу, передав принудительный id связанного маячка.
      )
    )
    Redirect( call )
  }

}
