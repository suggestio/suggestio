package io.suggest.sc.sjs.v.search

import io.suggest.sc.ScConstants.Search.Fts
import io.suggest.sc.sjs.c.FtsSearchCtl
import io.suggest.sjs.common.view.safe.css.SafeCssElT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.{Event, KeyboardEvent}
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 18:12
 * Description: Поле полнотекстового поиска довольно нетривиально внутри и будет расти.
 * Тут -- управление представлением этого поля и окружающих элементов.
 */
@deprecated("Use SInput and SInputContainer instead.", "2015.aug.7")
object FtsFieldView {
  
  /** Инициализация поля полнотекстового поиска. */
  @deprecated("Use SInput.initLayout() instead", "2015.aug.6")
  def initLayout(fieldSafe: SafeEventTargetT): Unit = {
    // Навешиваем события
    fieldSafe.addEventListener("keyup") { (e: KeyboardEvent) =>
      FtsSearchCtl.onFieldKeyUp(e)
    }
    fieldSafe.addEventListener("focus") { (e: Event) =>
      FtsSearchCtl.onFieldFocus(e)
    }
    fieldSafe.addEventListener("blur") { (e: Event) =>
      FtsSearchCtl.onFieldBlur(e)
    }
  }


  @deprecated("Use SInputContainer.activate() instead.", "2015.aug.7")
  def activateField(contSafe: SafeCssElT): Unit = {
    contSafe.addClasses( Fts.ACTIVE_INPUT_CLASS )
  }

  @deprecated("Use SInputContainer.deactivate() instead.", "2015.aug.7")
  def deactivateField(contSafe: SafeCssElT): Unit = {
    contSafe.removeClass( Fts.ACTIVE_INPUT_CLASS )
  }

  /** Выставить текст в поисковое поле. */
  @deprecated("Use SInput.setText() instead", "2015.aug.7")
  def setFtsFieldText(field: HTMLInputElement, t: String): Unit = {
    field.value = t
  }

}
