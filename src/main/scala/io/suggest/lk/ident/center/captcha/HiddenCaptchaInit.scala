package io.suggest.lk.ident.center.captcha

import io.suggest.sjs.common.controller.InitRouter
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import io.suggest.captcha.CaptchaConstants._

import scala.concurrent.Future
import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.15 18:02
 * Description: Страница ident содержит скрытую капчу. Эту капчу надо отображать при активности
 * в полях формы регистрации.
 * Чтобы браузер юзера не грузил такую капчу, img src также скрыт в data-src.
 */
trait HiddenCaptchaInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.HiddenCaptcha) {
      Future {
        showHiddenCaptchaOnFormInputsActivity()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

  private def container = jQuery("#" + HIDDEN_CAPTCHA_DIV_ID)

  /** Запуск инициализации поддержки скрытой капчи. */
  private def showHiddenCaptchaOnFormInputsActivity(): Unit = {
    val cont = container
    val form = cont.parent("form")
    val inputs = form.find("input:visible")
    if (inputs.`val`().toString.nonEmpty) {
      handleFormFocus()
    } else {
      inputs.on("focus", { e: JQueryEventObject =>
        handleFormFocus()
      }: ThisFunction)
    }
  }

  private def handleFormFocus(): Unit = {
    val cont = container
    if (!cont.is(":visible")) {
      val _disabled = "disabled"
      val form = cont.parent("form")
      val btns = form.find("input[type='submit']")
      def showCont(): Unit = {
        cont.show()
        btns.removeAttr(_disabled)
      }
      val imgHidden = cont.find("img[" + ATTR_HIDDEN_SRC + "]")
      if (imgHidden.length > 0) {
        // Есть скрытый url для капчи. Залить его в src и дождаться получения капчи, и только тогда отобразить скрытую капчу.
        val ahs = ATTR_HIDDEN_SRC
        val caUrl = imgHidden.attr(ahs).get
        imgHidden.attr("src", caUrl)
        imgHidden.removeAttr(ahs)
        // Запретить кнопке сабмита срабатывать.
        btns.attr(_disabled, _disabled)
        imgHidden.on("load", { (eload: JQueryEventObject) =>
          showCont()
        })
      } else {
        showCont()
      }
    }
  }

}
