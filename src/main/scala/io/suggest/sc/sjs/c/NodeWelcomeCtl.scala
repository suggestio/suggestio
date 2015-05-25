package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.v.welcome.NodeWelcomeView
import io.suggest.sjs.common.view.safe.css.SafeCssEl
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:17
 * Description: Контроллер для реакций на происходящее с карточкой приветствия узла.
 */
object NodeWelcomeCtl extends CtlT {

  /** Сработал таймер таймаута отображения приветствия. */
  def displayTimeout(rootEl: HTMLDivElement): Unit = {
    val safeEl = SafeCssEl(rootEl)
    if ( !NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация скрытия ещё не началась. Запустить анимацию.
      NodeWelcomeView.fadeOut(safeEl)
    }
  }

  /** Юзер кликает по отображенной карточке приветствия. Нужно её скрыть не дожидаясь таймера.
    * Если скрытие уже началось, то нужно вообще немедленно стереть. */
  def clicked(e: Event, rootEl: HTMLDivElement): Unit = {
    val safeEl = SafeCssEl(rootEl)
    if ( NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация уже началась. Значит надо спилить этот элемент по-скорее
      NodeWelcomeView.removeWelcome(rootEl)
    } else {
      // Анимация ещё не начиналась -- запустить анимацию.
      NodeWelcomeView.fadeOut(safeEl)
    }
  }

}
