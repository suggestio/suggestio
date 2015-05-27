package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mdom.listen.MListeners
import io.suggest.sc.sjs.m.mdom.listen.kbd.IKeyUpListener
import io.suggest.sc.sjs.m.mwc.{MWelcomeState, MWcDom}
import io.suggest.sc.sjs.v.welcome.NodeWelcomeView
import io.suggest.sjs.common.model.kbd.KeyCodes
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import org.scalajs.dom
import org.scalajs.dom.{Element, KeyboardEvent, Event}
import org.scalajs.dom.raw.HTMLDivElement

import scala.concurrent.{Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:17
 * Description: Контроллер для реакций на происходящее с карточкой приветствия узла.
 */
object NodeWelcomeCtl extends CtlT with IKeyUpListener {

  /** Сработал таймер таймаута отображения приветствия. */
  def displayTimeout(rootEl: HTMLDivElement): Unit = {
    val safeEl = SafeEl(rootEl)
    if ( !NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация скрытия ещё не началась. Запустить анимацию.
      NodeWelcomeView.fadeOut(safeEl)
    }
  }

  def clicked(e: Event): Unit = {
    MWcDom.rootDiv().foreach { rootDiv =>
      clicked(e, SafeEl(rootDiv))
    }
  }

  /**
   * Завершено сокрытие карточки.
   * @param rootDiv основной div welcome-карточки.
   */
  def hidingFinished(rootDiv: Element): Unit = {
    // Родительский элемент может быть null, если элемент уже удален.
    val parent = rootDiv.parentNode
    if (parent != null) {
      NodeWelcomeView.removeWelcome(rootDiv, parent)
      welcomeRemoved()
    }
  }

  /** Юзер кликает по отображенной карточке приветствия. Нужно её скрыть не дожидаясь таймера.
    * Если скрытие уже началось, то нужно вообще немедленно стереть. */
  def clicked(e: Event, safeEl: SafeCssElT { type T = HTMLDivElement }): Unit = {
    if ( NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация уже началась. Значит надо спилить этот элемент по-скорее
      hidingFinished(safeEl._underlying)
    } else {
      // Анимация ещё не начиналась -- запустить анимацию.
      NodeWelcomeView.fadeOut(safeEl)
    }
  }

  /** Карточка приветствия была удалена со страницы. Сняться с подписки на события. */
  private def welcomeRemoved(): Unit = {
    MListeners.removeKeyUpListener(this)
  }

  /** Вызывается, если обнаружилось, что карточка отображена на экране. */
  private def welcomeShown(): Unit = {
    MListeners.addKeyUpListener(this)
  }

  /**
   * При нажатии некоторых кнопок нужно ускорять сокрытие welcome.
   * @param e Событие клавиатуры.
   */
  override def handleKeyUp(e: KeyboardEvent): Unit = {
    val kc = e.keyCode
    if (kc == KeyCodes.ESC || kc == KeyCodes.ENTER || kc == KeyCodes.SPACE) {
      clicked(e)
    }
  }


  /**
   * Welcome-карточка ВОЗМОЖНО уже присутствует в DOM. Если присутствует, то значит отображена.
   * Нужно допилить карточку под экран, задать правила для сокрытия этой карточки через таймер или иные события.
   * @return Future, которые исполняется с началом анимации сокрытия welcome.
   *         Если welcome отсутствует, то Future придет уже исполненым.
   */
  def handleWelcome(): Future[_] = {
    val startHidingP = Promise[None.type]()
    def hidingStarted(): Unit = {
      if (!startHidingP.isCompleted)
        startHidingP success None
    }
    MWcDom.rootDiv() match {
      // Есть карточка в DOM. Подогнать по экран, повесить события.
      case Some(rootEl) =>
        NodeWelcomeView.fit()
        welcomeShown()

        val safeEl = SafeEl(rootEl)
        NodeWelcomeView.willHideAnimated(safeEl)

        // Запустить скрытие карточки по таймауту.
        dom.setTimeout(
          { () =>
            displayTimeout(rootEl)
            hidingStarted()
          },
          MWelcomeState.HIDE_TIMEOUT_MS
        )

        // Вешаем события ускоренного ухода с приветствия.
        safeEl.addEventListener("click") { (evt: Event) =>
          clicked(evt, safeEl)
          hidingStarted()
        }
        // TODO Нужно реагировать на "смахивание" приветствия.

      // Нет welcome карточки вообще.
      case None =>
        hidingStarted()
    }
    startHidingP.future
  }

}
