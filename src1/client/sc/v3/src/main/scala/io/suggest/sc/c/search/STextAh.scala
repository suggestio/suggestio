package io.suggest.sc.c.search

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.spa.DiodeUtil.Implicits.PotOpsExt
import io.suggest.sc.m.search._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.17 11:44
  * Description: Контроллер поля текстового поиска.
  */
class STextAh[M](
                  modelRW: ModelRW[M, MScSearchText]
                )
  extends ActionHandler( modelRW )
{

  /** Сколько времени ждать после ввода какого-то текста в поле?
    * Это фильтр для быстрых нажатий, например юзер давит backspace - ждём.
    */
  private def FAST_TYPING_TIMEOUT_MS = 600

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о вводе текста в поле текстового поиска.
    case m: SearchTextChanged =>
      val v0 = value
      if (v0.query ==* m.newText && !v0.searchQuery.isPending) {
        // Видимо, ничего не изменилось, либо был запущен какой-то таймер поиска на предыдущих итерациях.
        noChange

      } else {
        // Если уже запущен таймер фильтра быстрых нажатий, то загасить его.
        for (oldTimerId <- v0.searchTimerId)
          DomQuick.clearTimeout( oldTimerId )

        // Текст для поиска изменился.
        val v1 = v0.withQuery( m.newText )

        // Есть какой-то текст для запуска поиска. Запустить таймер.
        val tstamp = System.currentTimeMillis()
        val searchQueryPot2 = v1.searchQuery.pending( tstamp )

        // Запустить новый таймер, для фильтрации быстрых нажатий.
        val tp = DomQuick.timeoutPromiseT(FAST_TYPING_TIMEOUT_MS)( SearchTextTimerOut( tstamp ) )

        val fx = Effect( tp.fut )

        val v2 = v1.withSearchQueryTimer(
          searchQuery   = searchQueryPot2,
          searchTimerId = Some( tp.timerId )
        )

        updated( v2, fx )
      }


    // Сработал таймер запуска поисковых действий, связанных с возможным текстом в поле.
    case m: SearchTextTimerOut =>
      val v0 = value

      // Отфильтровать устаревший таймер:
      if (v0.searchQuery isPendingWithStartTime m.timestamp) {
        // Нужно понять, есть ли какой-либо текст, выставив итог в поле searchQuery.
        // Если нет, то очистить поле, выставив Pot.empty.
        val searchQueryPot2 = if (v0.query.isEmpty || v0.query.trim.isEmpty) {
          Pot.empty[String]
        } else {
          v0.searchQuery.ready( v0.query )
        }
        // Обновить состояние.
        val v2 = v0.withSearchQueryTimer(
          searchQuery   = searchQueryPot2,
          searchTimerId = None
        )

        if (v0.searchQuery contains v0.query) {
          // Ничего не изменилось в итоге, активировать поиск не требуется.
          updatedSilent(v2)
        } else {
          // Что-то надо искать, запустить экшен поиска.
          val fx = Effect.action( ReDoSearch )
          updatedSilent(v2, fx)
        }

      } else {
        // Сигнал от какого-то неактуального таймера. Игнорим.
        noChange
      }


    // Сигнал о focus/blur от текстового поля.
    case m: SearchTextFocus =>
      val v0 = value
      if (v0.focused !=* m.focused && v0.query.isEmpty) {
        val v2 = v0.withFocused( m.focused )
        updated( v2 )
      } else {
        noChange
      }

  }

}
