package io.suggest.xadv.ext.js.runner.v

import io.suggest.adv.ext.view.RunnerPage._
import org.scalajs.dom
import org.scalajs.jquery._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 10:10
 * Description: Поддержка действий на страницы вынесена в view, но это очень условно.
 * Тут события DOM на runner-странице и реакция на них.
 */
object Page {

  /** Подписка на необходимые DOM-события страницы runner. */
  def bindPageEvents(): Unit = {
    val evtsContainer = jQuery(s"#$ID_EVTS_CONTAINER")
    bindShowErrorInfo(evtsContainer)
  }

  /** Забиндить событие клика по линку отображения ошибки. */
  def bindShowErrorInfo(container: JQuery): Unit = {
    container.on("click", s".$CLASS_JSLINK_SHOW_ERROR", {
      e: JQueryEventObject =>
        jQuery(e.currentTarget)
          .next()
          .slideToggle()
    })
  }

  /** Запустить этот код, когда страница будет готова. */
  def onReady(f: () => _): Unit = {
    jQuery(dom.document)
      .ready(f: js.Function0[_])
  }


  /** Запустить переданный код при закрытии/обновлении страницы. */
  def onClose(f: () => _): Unit = {
    jQuery(dom.window)
      .on("beforeunload", f: js.Function0[_])
  }

}
