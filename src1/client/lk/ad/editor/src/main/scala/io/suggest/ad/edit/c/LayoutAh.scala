package io.suggest.ad.edit.c

import diode.{ActionHandler, ActionResult, ModelRO, ModelRW}
import io.suggest.ad.edit.m.layout.MLayoutS
import io.suggest.ad.edit.m.{HandleVScroll, MDocS}
import io.suggest.common.event.DomEvents
import io.suggest.sjs.common.vm.doc.DocumentVm
import org.scalajs.dom._
import io.suggest.sjs.common.vm.evtg.EventTargetVm._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.10.17 17:44
  * Description: Контроллер воздействия на внешность редактора.
  */
class LayoutAh[M](
                   modelRW    : ModelRW[M, MLayoutS],
                   docRO      : ModelRO[MDocS]
                 )
  extends ActionHandler(modelRW) {

  def subscribeForScroll(etg: EventTarget, dispatcher: diode.Dispatcher): Unit = {
    etg.addEventListener4s( DomEvents.SCROLL ) { _: Event =>
      // TODO Opt Отфильтровать события горизонтального скроллинга.
      val d = DocumentVm()
      // Есть проблемы со скроллингом между firefox и chrome.
      val scrollTop = d.scrollingElement.scrollTop
      dispatcher( HandleVScroll(scrollTop) )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сообщение о скроллинге страницы.
    case m: HandleVScroll =>
      val v0 = value
      val scrollY = m.y.toInt
      if (scrollY <= v0.rightPanelTop) {
        // Слишком малый скроллинг. Значение координаты должно быть None.
        _setNoY(v0)

      } else {
        // Новая координата верхушки без фильтрации максимальным значением
        val newY0 = scrollY - v0.rightPanelTop

        // Пора двигать правую панель по вертикали. Но с учётом ограничения высоты документа.
        val docHeight = docRO.value.jdArgs.templateHeightCssPx
        // TODO Нужна реальная фактическая контейнера редактора или текущего выделенного элемента.
        // Нужно отцентровать контейнер редакторов относительно текущего выделенного элемента.
        val editorContHeightCssPx = 350

        val maxY = docHeight - editorContHeightCssPx
        val newY2 = Math.max(0,
          Math.min(maxY, newY0)
        )

        if (newY2 <= 0) {
          _setNoY(v0)
        } else if ( v0.rightPanelY.contains(newY2.toDouble) ) {
          noChange
        } else {
          val v2 = v0.withRightPanelY(
            Some( newY2 )
          )
          updated(v2)
        }
      }

  }


  /** Отключить позиционирование под экран. */
  private def _setNoY(v0: MLayoutS): ActionResult[M] = {
    if (v0.rightPanelY.isEmpty) {
      noChange
    } else {
      val v2 = v0.withRightPanelY( None )
      updated(v2)
    }
  }

}
