package controllers.clk

import controllers.SioController
import io.suggest.i18n.I18nConst
import io.suggest.util.logs.IMacroLogs
import play.api.i18n.Messages
import util.acl.IMaybeAuth
import util.i18n.IJsMessagesUtilDi

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 16:20
  * Description: Аддон для [[controllers.LkLang]] для поддержки экшена, раздающего js с языковыми кодами
  * для client-side локализации.
  */
trait LkJsMessages
  extends SioController
  with IMaybeAuth
  with IJsMessagesUtilDi
  with IMacroLogs
{

  /** Глобальное имя на клиенте, в которое будет залита функция локализации. */
  private def JS_NAME = "window." + I18nConst.MESSAGES_JSNAME

  /** Сколько секунд кэшировать на клиенте js'ник с локализацией. */
  private val CACHE_MAX_AGE_SECONDS = if (mCommonDi.isProd) 864000 else 5



  /** 2016.dec.6: Из-за опытов с react.js возникла необходимость использования client-side messages.
    * Тут экшен, раздающий messages для личного кабинета.
    *
    * @param hash PROJECT LAST_MODIFIED hash code.
    * @param langCode Изначальное не проверяется, но для решения проблем с кешированием вбит в адрес ссылки.
    * @return js asset с локализованными мессагами внутрях.
    */
  def lkMessagesJs(langCode: String, hash: Int) = maybeAuth().async { implicit request =>

    // Проверить хеш
    if (hash == jsMessagesUtil.hash) {
      val messages = implicitly[Messages]

      // Проверить langCode
      if (messages.lang.code equalsIgnoreCase langCode) {
        val js = jsMessagesUtil.lkJsMsgsFactory(Some(JS_NAME))(messages)
        Ok(js)
          .withHeaders(CACHE_CONTROL -> ("public, max-age=" + CACHE_MAX_AGE_SECONDS))

      } else {
        NotFound("lang: " + langCode)
      }

    } else {
      LOGGER.trace(s"${request.path} hash=$hash must be ${jsMessagesUtil.hash}")
      NotFound("hash: " + hash)
    }
  }

}
