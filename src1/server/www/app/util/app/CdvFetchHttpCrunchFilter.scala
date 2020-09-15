package util.app

import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.text.StringUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import play.api.http.HeaderNames.X_REQUESTED_WITH
import play.api.http.HttpEntity
import play.api.mvc.{EssentialAction, EssentialFilter}

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.2020 13:39
  * Description: Фильтр, содержащий костыли запросов от cordova-fetch-плагина,
  * который крайне хреново поддерживает HTTP.
  */
final class CdvFetchHttpCrunchFilter @Inject() (
                                                 implicit private val ec: ExecutionContext
                                               )
  extends EssentialFilter
  with MacroLogsImpl
{

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { rh0 =>
      var respFut = next(rh0)

      // если тут у нас приложение, то нужно возвращать тело, хотя бы пустое, но никаких NoEntity
      val isApp: Boolean = rh0.headers
        .get( X_REQUESTED_WITH )
        .exists( _ endsWith HttpConst.Headers.XRequestedWith.XRW_APP_SUFFIX )

      // Если запрос из аппликухи, а тело ответа абсолютно пустое, то нужно вернуть тело-пустышку.
      if (isApp) {
        respFut = respFut.map { result =>
          result.body match {
            case HttpEntity.NoEntity =>
              result.as( MimeConst.TEXT_PLAIN )
            case _ => result
          }
        }
        LOGGER.trace(s"${rh0.method} ${StringUtil.strLimitLen(rh0.uri, 32)} : resetting empty-body into zero-len, because isApp?$isApp")
      }

      respFut
    }
  }

}
