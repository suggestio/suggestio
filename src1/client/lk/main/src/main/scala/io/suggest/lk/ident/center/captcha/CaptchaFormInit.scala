package io.suggest.lk.ident.center.captcha

import io.suggest.captcha.CaptchaConstants._
import io.suggest.init.routed.InitRouter
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}
import japgolly.univeq._

import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.15 15:55
 * Description: Инициализатор поддержки и сама поддержка для формы ввода капчи.
 */
trait CaptchaFormInit extends InitRouter {

  /** Инициализация одной цели. IR-аддоны должны перезаписывать по цепочке этот метод своей логикой. */
  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.CaptchaForm) {
      initForm()
    } else {
      super.routeInitTarget(itg)
    }
  }

  /** Выполнить инициализацию формы для ввода капчи. */
  private def initForm(): Unit = {
    val typInput = jQuery("#" + CAPTCHA_TYPED_INPUT_ID)
    _handleReloadClick()
    _trimTypedOnBlur(typInput)
    _ensureCapthaTypedEmpty(typInput)
  }

  private def _ensureCapthaTypedEmpty(typInput: JQuery): Unit = {
    typInput.`val`("")
  }

  /** Реакция на кнопку релоада капчи. */
  private def _handleReloadClick(): Unit = {
    jQuery("#" + CAPTCHA_RELOAD_BTN_ID)
      .on("click", {(that: HTMLElement, e: JQueryEventObject) =>
        e.preventDefault()
        val forImg = jQuery( that.getAttribute(ATTR_RELOAD_FOR) )
        val srcAttr = "src"
        val src0 = forImg.attr(srcAttr).get
        val src1 = src0 + (if (src0.indexOf('?') > 0) "&" else "?") + "rnd=" + Math.random()
        forImg.attr(srcAttr, src1)
      }: ThisFunction)
  }

  /** Удалять лишние пробелы из введенной капчи в поле. Это не обязательно. */
  private def _trimTypedOnBlur(typeInput: JQuery): Unit = {
    typeInput
      .on("blur", { (that: HTMLElement, e: JQueryEventObject) =>
        val jqThat = jQuery(that)
        val raw = jqThat.`val`().toString
        val trimmed = raw.trim
        if (trimmed.length < raw.length)
          jqThat.`val`(trimmed)
      }: ThisFunction)
  }

}
