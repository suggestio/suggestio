package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.CtlT
import io.suggest.sc.sjs.m.mdom.listen.MListeners
import io.suggest.sc.sjs.m.mwc.{WcHidePromise_t, SafeRootDiv_t, MWelcomeState, MWcDom}
import io.suggest.sc.sjs.v.welcome.NodeWelcomeView
import io.suggest.sjs.common.model.kbd.KeyCodes
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom
import org.scalajs.dom.{Element, Event}

import scala.concurrent.{Future, Promise}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:17
 * Description: Контроллер для реакций на происходящее с карточкой приветствия узла.
 */
object NodeWelcomeCtl extends CtlT {

  private def KEY_LISTENER_ID = getClass.getSimpleName

  /** Сработал таймер таймаута отображения приветствия. */
  def displayTimeout(safeEl: SafeRootDiv_t, p: WcHidePromise_t): Unit = {
    if ( !NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация скрытия ещё не началась. Запустить анимацию.
      _startHiding(safeEl, p)
    }
  }

  /** Запуск сокрытия карточки. */
  private def _startHiding(safeEl: SafeRootDiv_t, p: WcHidePromise_t): Unit = {
    NodeWelcomeView.fadeOut(safeEl)
    finishP(p)
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
      MListeners.removeKeyUpListener(KEY_LISTENER_ID)
    }
  }


  /** Юзер кликает по отображенной карточке приветствия. Нужно её скрыть не дожидаясь таймера.
    * Если скрытие уже началось, то нужно вообще немедленно стереть. */
  def clicked(e: Event, safeEl: SafeRootDiv_t, p: WcHidePromise_t): Unit = {
    if ( NodeWelcomeView.isAnimatedNow(safeEl) ) {
      // Анимация уже началась. Значит надо спилить этот элемент по-скорее
      hidingFinished(safeEl._underlying)
    } else {
      // Анимация ещё не начиналась -- запустить анимацию.
      _startHiding(safeEl, p)
    }
  }


  private def finishP(p: WcHidePromise_t): Unit = {
    if (!p.isCompleted)
      p success None
  }

  private def isHideOnKey(keyCode: Int): Boolean = {
    keyCode == KeyCodes.ESC || keyCode == KeyCodes.ENTER || keyCode == KeyCodes.SPACE
  }

  /**
   * Welcome-карточка ВОЗМОЖНО уже присутствует в DOM. Если присутствует, то значит отображена.
   * Нужно допилить карточку под экран, задать правила для сокрытия этой карточки через таймер или иные события.
   * @return Future, которые исполняется с началом анимации сокрытия welcome.
   *         Если welcome отсутствует, то Future придет уже исполненым.
   */
  def handleWelcome(): Future[_] = {
    val p: WcHidePromise_t = Promise[None.type]()
    MWcDom.rootDiv() match {
      // Есть карточка в DOM. Подогнать по экран, повесить события.
      case Some(rootEl) =>
        NodeWelcomeView.fit()

        val safeEl: SafeRootDiv_t = SafeEl(rootEl)

        // Подписываемся на события клавиатуры
        MListeners.addKeyUpListener(KEY_LISTENER_ID) { e =>
          if ( isHideOnKey(e.keyCode) ) {
            clicked(e, safeEl, p)
          }
        }

        NodeWelcomeView.willHideAnimated(safeEl)

        // Запустить скрытие карточки по таймауту.
        dom.setTimeout(
          { () => displayTimeout(safeEl, p) },
          MWelcomeState.HIDE_TIMEOUT_MS
        )

        // Вешаем события ускоренного ухода с приветствия.
        safeEl.addEventListener("click") { (evt: Event) =>
          clicked(evt, safeEl, p)
        }
        // TODO Нужно реагировать на "смахивание" приветствия.

      // Нет welcome карточки вообще.
      case None =>
        finishP(p)
    }
    p.future
  }

}
