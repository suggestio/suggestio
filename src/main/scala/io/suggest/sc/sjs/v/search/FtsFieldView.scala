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
object FtsFieldView {
  
  /** Инициализация поля полнотекстового поиска. */
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



  def activateField(contSafe: SafeCssElT): Unit = {
    contSafe.addClasses( Fts.ACTIVE_INPUT_CLASS )
  }

  def deactivateField(contSafe: SafeCssElT): Unit = {
    contSafe.removeClass( Fts.ACTIVE_INPUT_CLASS )
  }

  /** Выставить текст в поисковое поле. */
  def setFtsFieldText(field: HTMLInputElement, t: String): Unit = {
    field.value = t
  }

}
