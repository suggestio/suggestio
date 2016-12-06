package controllers.clk

import controllers.SioController
import io.suggest.dt.interval.PeriodsConstants
import io.suggest.i18n.I18nConstants
import io.suggest.model.mproj.IMProjectInfo
import jsmessages.JsMessages
import models.adv.form.QuickAdvPeriods
import play.api.i18n.Messages
import util.acl.MaybeAuth

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 16:20
  * Description: Аддон для [[controllers.LkLang]] для поддержки экшена, раздающего js с языковыми кодами
  * для client-side локализации.
  */
trait LkJsMessages
  extends SioController
  with MaybeAuth
  with IMProjectInfo
{

  import mCommonDi.jsMessagesFactory


  /** Глобальное имя на клиенте, в которое будет залита функция локализации. */
  private def JS_NAME = "window." + I18nConstants.LK_MESSAGES_JSNAME

  /** Сколько секунд кэшировать на клиенте js'ник с локализацией. */
  private val CACHE_MAX_AGE_SECONDS = if (mCommonDi.isProd) 864000 else 10


  /** Локализация для периодов рекламного размещения. */
  private def _advPeriodMsgs: TraversableOnce[String] = {
    val static = Iterator(
      "Date.choosing",
      "Advertising.period",
      "Your.ad.will.adv",
      "from._date",
      "till._date",
      "Date.start",
      "Date.end"
    )

    val advPeriodsIter: Iterator[String] = {
      Seq(
        QuickAdvPeriods.valuesT
          .iterator
          .map(_.messagesCode),
        Seq( PeriodsConstants.MESSAGES_PREFIX + PeriodsConstants.CUSTOM )
      )
        .iterator
        .flatten
    }

    Iterator(static, advPeriodsIter)
      .flatten
  }


  /** Готовенькие сообщения для раздачи через js сообщения на всех поддерживаемых языках. */
  private val _lkJsMsgs: JsMessages = {
    val msgs = Iterator(
      _advPeriodMsgs
    )
      .flatten
      .toSeq
    jsMessagesFactory.subset( msgs: _* )
  }


  /** 2016.dec.6: Из-за опытов с react.js возникла необходимость использования client-side messages.
    * Тут экшен, раздающий messages для личного кабинета.
    *
    * @param hash PROJECT LAST_MODIFIED hash code.
    * @param langCode Изначальное не проверяется, но для решения проблем с кешированием вбит в адрес ссылки.
    * @return js asset с локализованными мессагами внутрях.
    */
  def lkMessagesJs(langCode: String, hash: Int) = MaybeAuth().async { implicit request =>
    val currHash = mProjectInfo.PROJECT_CODE_LAST_MODIFIED.hashCode()

    // Проверить хеш
    if (hash == currHash) {
      val sessionMessages = implicitly[Messages]

      // Проверить langCode
      if (sessionMessages.lang.code equalsIgnoreCase langCode) {
        val js = _lkJsMsgs.apply(Some(JS_NAME))(sessionMessages)
        Ok(js)
          .withHeaders(CACHE_CONTROL -> ("public, max-age=" + CACHE_MAX_AGE_SECONDS))

      } else {
        NotFound("lang: " + langCode)
      }

    } else {
      NotFound("hash: " + hash)
    }
  }

}
