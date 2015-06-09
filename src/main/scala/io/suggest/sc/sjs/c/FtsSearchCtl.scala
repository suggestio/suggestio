package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.msc.fsm.MScFsm
import io.suggest.sc.sjs.m.msearch.{MFtsSearchCtx, MSearchDom}
import io.suggest.sc.sjs.v.search.FtsFieldView
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 18:11
 * Description: Контроллер для поля полнотекстового поиска.
 */
object FtsSearchCtl extends SjsLogger {

  /** Инициализация подсистемы после перерисовки layout'а. */
  def initNodeLayout(): Unit = {
    // Инициализация search-панели: реакция на кнопку закрытия/открытия, поиск при наборе, табы и т.д.
    for (input <- MSearchDom.ftsInput) {
      FtsFieldView.initLayout( SafeEl(input) )
    }
  }

  /** Фокус попал в поле поиска. */
  def onFieldFocus(e: Event): Unit = {
    val st0 = MScFsm.state
    if (st0.ftsSearch.isEmpty) {
      // Юзер намеревается начать поиск
      MScFsm.pushState(st0.copy(
        ftsSearch = Some(MFtsSearchCtx())
      ))
      MScFsm.applyStateChanges()
    }
  }

  def onFieldKeyUp(e: Event): Unit = {
    error("TODO") // TODO
  }

  def onFieldBlur(e: Event): Unit = {
    error("TODO") // TODO
  }

  /**
   * Возможно, произошло изменение состояния fts. Здесь происходит анализ произошедших изменений
   * и внесение измений в представление FTS-поля.
   * @param oldState Старое состояние.
   * @param newState Новое состояние.
   */
  def maybeFtsStateChanged(oldState: Option[MFtsSearchCtx], newState: Option[MFtsSearchCtx]): Unit = {
    val oldEmpty = oldState.isEmpty
    val currEmpty = newState.isEmpty
    val inputOpt = MSearchDom.ftsInput

    for (input <- inputOpt) {
      val safeInput = SafeEl( input )

      if (!oldEmpty && currEmpty) {
        // Сброс полнотекстового поиска. Сбросить сетку, сбросить поле поиска.
        GridCtl.reFindAds()
        FtsFieldView.setFtsFieldText(input, "")
        FtsFieldView.deactivateField(safeInput)
        // TODO Удалить возможное сообщение об ошибке в запросе.

      } else if (!currEmpty) {
        // Полнотексовый поиск стал/остался активен. Нужно накатить изменения состояния.
        val curr = newState.get

        // Визуально активировать поле, если не было активно.
        if (oldEmpty) {
          FtsFieldView.activateField(safeInput)
        }

        // TODO Отрабатываем поле q, содержащее текстовый запрос.
        //if (oldEmpty && curr.q.length > 0) {
          // Имело место резкое изменение текста, то надо выставить его в поле.
        ???
      }
    }
  }

}
