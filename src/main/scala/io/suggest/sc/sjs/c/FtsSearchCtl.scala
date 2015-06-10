package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.cutil.FtsFsm
import io.suggest.sc.sjs.m.msearch.MSearchDom
import io.suggest.sc.sjs.v.search.FtsFieldView
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.common.view.safe.SafeEl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 18:11
 * Description: Контроллер для поля полнотекстового поиска.
 */
object FtsSearchCtl extends FtsFsm with SjsLogger {

  /** Инициализация подсистемы после перерисовки layout'а. */
  def initNodeLayout(): Unit = {
    // Инициализация search-панели: реакция на кнопку закрытия/открытия, поиск при наборе, табы и т.д.
    for (input <- MSearchDom.ftsInput) {
      FtsFieldView.initLayout( SafeEl(input) )
    }
  }


  /**
   * Возможно, произошло изменение состояния fts. Здесь происходит анализ произошедших изменений
   * и внесение измений в представление FTS-поля.
   * @param oldState Старое состояние.
   * @param newState Новое состояние.
   */
  def maybeFtsStateChanged(oldState: Option[String], newState: Option[String]): Unit = {
    val oldEmpty = oldState.isEmpty
    val currEmpty = newState.isEmpty
    val inputOpt = MSearchDom.ftsInput
    val inputContOpt = MSearchDom.ftsInputContainerDiv

    for (input <- inputOpt) {
      val inputContSafeOpt = inputContOpt.map( SafeEl.apply )

      if (!oldEmpty && currEmpty) {
        // Сброс полнотекстового поиска. Сбросить сетку, сбросить поле поиска.
        resetFts(inputOpt)

      } else if (!currEmpty) {
        // Полнотексовый поиск стал/остался активен. Нужно накатить изменения состояния.
        val curr = newState.get

        // Визуально активировать поле, если не было активно.
        if (oldEmpty || oldState.get.length <= 0) {
          for (inputCont <- inputContSafeOpt) {
            FtsFieldView.activateField( inputCont )
          }
        }

        // Отрабатываем поле q, содержащее текстовый запрос.
        val newQLen = curr.length

        // Если имело место резкое изменение состояния, то выставить текст из состояния в поле.
        // Такое возможно при импорте состояния из истории/URL.
        if (oldEmpty && newQLen > 1) {
          FtsFieldView.setFtsFieldText(input, curr)
        }

        // Если новое состояние содержит подходящий текст, отличающийся от предшествующего варианта, нужно активировать поиск.
        if (newQLen > 1 && !oldState.contains(curr)) {
          startFindReq()
        }
        // TODO При нулевой длине запроса нужно запрашивать исходную выдачу
      }
    }
  }

}
