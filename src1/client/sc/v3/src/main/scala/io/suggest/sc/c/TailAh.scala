package io.suggest.sc.c

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.GetRouterCtlF
import io.suggest.sc.m.{MScRoot, ResetUrlRoute, RouteTo}
import io.suggest.sc.m.Sc3Pages.MainScreen
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.hdr.HSearchBtnClick
import io.suggest.sc.m.inx.{GetIndex, WcTimeOut}
import io.suggest.sc.m.search.{SwitchTab, TagClick}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
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
      val searchOpened = v0.index.search.isShown
      val currRcvrId = inxState.currRcvrId
      val m = MainScreen(
        nodeId        = currRcvrId,
        // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
        // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
        locEnv        = OptionUtil.maybe {
          currRcvrId.isEmpty || v0.index.search.isShown || v0.index.search.tags.selectedId.nonEmpty
        }(v0.index.search.mapInit.state.center),
        generation    = Some( inxState.generation ),
        searchOpened  = searchOpened,
        searchTab = OptionUtil.maybe(searchOpened)( v0.index.search.currTab ),
        tagNodeId     = v0.index.search.tags.selectedId
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
      var needUpdateUi = false

      var inx = v0.index

      var fxsAcc = List.empty[Effect]

      // Проверка id узла. Если отличается, то надо перезаписать.
      if (m.mainScreen.nodeId !=* inx.state.currRcvrId) {
        nodeIndexNeedsReload = true
        inx = inx.withState {
          inx.state.withRcvrNodeId( m.mainScreen.nodeId.toList )
        }
      }

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if generation2 !=* inx.state.generation
      } {
        // generation не совпадает. Надо будет перезагрузить плитку.
        gridNeedsReload = true
        inx = inx.withState(
          inx.state.withGeneration( generation2 )
        )
      }

      // Текущий открытый таб на панели поиска
      for (currSearchTab <- m.mainScreen.searchTab)
        fxsAcc ::= Effect.action( SwitchTab( currSearchTab ) )

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* v0.index.search.isShown) {
        // Вместо патчинга состояния имитируем клик: это чтобы возможные сайд-эффекты обычного клика тоже отработали.
        fxsAcc ::= Effect.action( HSearchBtnClick )
      }

      // Смотрим координаты текущей точки.
      for (currGeoPoint <- m.mainScreen.locEnv) {
        needUpdateUi = true
        inx = inx.withSearch(
          inx.search.withMapInit(
            inx.search.mapInit.withState(
              inx.search.mapInit.state
                .withCenterInitReal( currGeoPoint )
            )
          )
        )
      }

      // Смотрим текущий выделенный тег
      for (tagNodeId <- m.mainScreen.tagNodeId) {
        // Имитируем клик по тегу, да и всё.
        fxsAcc ::= Effect.action {
          TagClick( tagNodeId )
        }
      }

      // Обновлённое состояние, которое может быть и не обновлялось:
      lazy val v2 = v0.withIndex( inx )

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
