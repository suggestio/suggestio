package io.suggest.sc.root.c

import diode._
import io.suggest.react.ReactDiodeUtil
import io.suggest.sc.grid.m.GridLoadAds
import io.suggest.sc.inx.m.{GetIndex, WcTimeOut}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.root.m.{MScRoot, ResetUrlRoute, RouteTo}
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.GetRouterCtlF
import io.suggest.sc.Sc3Pages.MainScreen
import io.suggest.sc.hdr.m.HSearchBtnClick
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.17 12:53
  * Description: Бывает, что надо заглушить сообщения, т.к. обработка этих сообщений стала
  * неактуальной, а сообщения всё ещё идут.
  */
class TailAh[M](
                 modelRW              : ModelRW[M, MScRoot],
                 routerCtlF           : GetRouterCtlF
               )
  extends ActionHandler(modelRW)
{ ah =>

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Заставить роутер собрать новую ссылку.
    case ResetUrlRoute =>
      val v0 = value
      val inxState = v0.index.state
      val m = MainScreen(
        nodeId        = inxState.currRcvrId,
        generation    = Some( inxState.generation ),
        searchOpened  = v0.index.search.isShown
      )
      val routerCtl = routerCtlF()
      // TODO Opt Проверить, изменилась ли ссылка.
      routerCtl.set( m ).runNow()
      noChange


    // js-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      val v0 = value

      // Считаем, что js-роутер уже готов. Если нет, то это сообщение должно было быть перехвачено в JsRouterInitAh.

      var gridNeedsReload = false
      var nodeIndexNeedsReload = v0.index.resp.isTotallyEmpty
      val needUpdateUi = false

      var inxState = v0.index.state

      var fxsAcc = List.empty[Effect]

      // Проверка id узла. Если отличается, то надо перезаписать.
      if (m.mainScreen.nodeId !=* inxState.currRcvrId) {
        nodeIndexNeedsReload = true
        inxState = inxState.withRcvrNodeId( m.mainScreen.nodeId.toList )
      }

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if generation2 !=* inxState.generation
      } {
        // generation не совпадает. Надо будет перезагрузить плитку.
        gridNeedsReload = true
        inxState = inxState.withGeneration( generation2 )
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* v0.index.search.isShown) {
        // Вместо патчинга состояния имитируем клик: это чтобы возможные сайд-эффекты обычного клика тоже отработали.
        fxsAcc ::= Effect.action( HSearchBtnClick )
      }

      // Обновлённое состояние, которое может быть и не обновлялось:
      lazy val v2 = v0.withIndex(
        v0.index.withState(
          inxState
        )
      )

      // Принять решение о перезагрузке выдачи, если возможно.
      if (nodeIndexNeedsReload) {
        // Целиковая перезагрузка выдачи.
        fxsAcc ::= Effect.action {
          GetIndex( withWelcome = true )
        }
        val fx = ReactDiodeUtil.mergeEffectsSet( fxsAcc ).get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (gridNeedsReload) {
        // Узел не требует обновления, но требуется перезагрузка плитки.
        fxsAcc ::= Effect.action {
          GridLoadAds( clean = true, ignorePending = true )
        }
        val fx = ReactDiodeUtil.mergeEffectsSet( fxsAcc ).get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (needUpdateUi) {
        // Изменения на уровне интерфейса.
        val fxOpt = ReactDiodeUtil.mergeEffectsSet( fxsAcc )
        ah.updatedMaybeEffect( v2, fxOpt )

      } else {
        // Ничего не изменилось или только эффекты.
        val fxOpt = ReactDiodeUtil.mergeEffectsSet( fxsAcc )
        ah.maybeEffectOnly( fxOpt )
      }


    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange

  }

}
