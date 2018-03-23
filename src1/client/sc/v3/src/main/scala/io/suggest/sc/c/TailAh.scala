package io.suggest.sc.c

import diode._
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.GetRouterCtlF
import io.suggest.sc.m._
import io.suggest.sc.m.Sc3Pages.MainScreen
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.hdr.{MenuOpenClose, SearchOpenClose}
import io.suggest.sc.m.inx.{GetIndex, MScIndex, WcTimeOut}
import io.suggest.sc.m.search.{MSearchTabs, SwitchTab, TagClick}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

import scala.concurrent.Future

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

  def getMainScreenSnapShot(v0: MScRoot = value): MainScreen = {
    val inxState = v0.index.state
    val searchOpened = v0.index.search.isShown
    val currRcvrId = inxState.currRcvrId
    MainScreen(
      nodeId        = currRcvrId,
      // Не рендерить координаты в URL, если находишься в контексте узла, закрыта панель поиска и нет выбранного тега.
      // Это улучшит кэширование, возможно улучшит приватность при обмене ссылками.
      locEnv        = OptionUtil.maybe {
        currRcvrId.isEmpty || v0.index.search.isShown || v0.index.search.tags.selectedId.nonEmpty
      }(v0.index.search.mapInit.state.center),
      generation    = Some( inxState.generation ),
      searchOpened  = searchOpened,
      searchTab     = OptionUtil.maybe(searchOpened)( v0.index.search.currTab ),
      tagNodeId     = v0.index.search.tags.selectedId,
      menuOpened    = v0.index.menu.opened
    )
  }

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Заставить роутер собрать новую ссылку.
    case ResetUrlRoute =>
      val m = getMainScreenSnapShot()
      // Уведомить в фоне роутер, заодно разблокировав интерфейс.
      Future {
        routerCtlF()
          .set( m )
          .runNow()
      }

      noChange


    // js-роутер заливает в состояние данные из URL.
    case m: RouteTo =>
      val v0 = value
      val currMainScreen = getMainScreenSnapShot(v0)

      // Считаем, что js-роутер уже готов. Если нет, то это сообщение должно было быть перехвачено в JsRouterInitAh.

      var gridNeedsReload = false
      val indexRespTotallyEmpty = v0.index.resp.isTotallyEmpty
      var nodeIndexNeedsReload = indexRespTotallyEmpty
      var needUpdateUi = false

      var inx = v0.index

      var fxsAcc = List.empty[Effect]

      // Проверка id узла. Если отличается, то надо перезаписать.
      if (m.mainScreen.nodeId !=* currMainScreen.nodeId) {
        nodeIndexNeedsReload = true
        inx = inx.withState {
          inx.state.withRcvrNodeId( m.mainScreen.nodeId.toList )
        }
      }

      // Проверка поля generation
      for {
        generation2 <- m.mainScreen.generation
        if !currMainScreen.generation.contains( generation2 )
      } {
        // generation не совпадает. Надо будет перезагрузить плитку.
        gridNeedsReload = true
        inx = inx.withState(
          inx.state.withGeneration( generation2 )
        )
      }

      // Текущий открытый таб на панели поиска
      for {
        mSearchTab <- m.mainScreen.searchTab
        if !currMainScreen.searchTab.contains( mSearchTab )
      } {
        fxsAcc ::= Effect.action( SwitchTab( mSearchTab ) )
      }

      // Проверка поля searchOpened
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened) {
        // Вместо патчинга состояния имитируем клик: это чтобы возможные сайд-эффекты обычного клика тоже отработали.
        fxsAcc ::= Effect.action( SearchOpenClose(m.mainScreen.searchOpened, m.mainScreen.searchTab) )
      }
      // TODO Возможно, SwitchTab надо запихнуть в else-ветвь тут?

      // Смотрим координаты текущей точки.
      for {
        currGeoPoint <- m.mainScreen.locEnv
        if !currMainScreen.locEnv.contains(currGeoPoint)
      } {
        needUpdateUi = true
        inx = withMapCenter(currGeoPoint, inx)
      }

      // Смотрим текущий выделенный тег
      for {
        tagNodeId <- m.mainScreen.tagNodeId
        if !currMainScreen.tagNodeId.contains( tagNodeId )
      } {
        // Имитируем клик по тегу, да и всё.
        fxsAcc ::= Effect.action {
          TagClick( tagNodeId )
        }
      }

      // Сверить панель меню открыта или закрыта
      if (m.mainScreen.menuOpened !=* currMainScreen.menuOpened) {
        fxsAcc ::= Effect.action( MenuOpenClose(m.mainScreen.menuOpened) )
      }

      // Обновлённое состояние, которое может быть и не обновлялось:
      lazy val v2 = v0.withIndex( inx )

      // Принять решение о перезагрузке выдачи, если необходимо.
      if ( fxsAcc.isEmpty && !needUpdateUi && !gridNeedsReload && inx ===* v0.index) {
        // Роутер шлёт RouteTo на каждый чих, даже просто в ответ на выставление этой самой роуты.
        // TODO надо как-то отучить его от этой привычки.
        noChange

      } else if (v0.internals.geoLockTimer.nonEmpty) {
        // Блокирование загрузки.
        val fxOpt = fxsAcc.mergeEffectsSet
        ah.updatedSilentMaybeEffect(v2, fxOpt)

        // TODO Если новое состояние условно "пустое" (без rcvrId или координат), то запустить геолокацию, можно даже без таймера (поддерживается?), а просто запустить.
        // TODO Иначе - остановить геолокацию, если она запущена сейчас -- т.к. состояние "непустое".
      } else if (nodeIndexNeedsReload) {
        // Целиковая перезагрузка выдачи.
        fxsAcc ::= getIndexFx( geoIntoRcvr = false )
        val fx = fxsAcc.mergeEffectsSet.get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (gridNeedsReload) {
        // Узел не требует обновления, но требуется перезагрузка плитки.
        fxsAcc ::= Effect.action {
          GridLoadAds( clean = true, ignorePending = true )
        }
        val fx = fxsAcc.mergeEffectsSet.get
        ah.updateMaybeSilentFx(needUpdateUi)(v2, fx)

      } else if (needUpdateUi) {
        // Изменения на уровне интерфейса.
        val fxOpt = fxsAcc.mergeEffectsSet
        ah.updatedMaybeEffect( v2, fxOpt )

      } else {
        // Ничего не изменилось или только эффекты.
        val fxOpt = fxsAcc.mergeEffectsSet
        ah.maybeEffectOnly( fxOpt )
      }

    // Сигнал наступления геолокации (или ошибки геолокации).
    case m: GlPubSignal =>
      val v0 = value

      // Сейчас ожидаем максимально точных координат?
      v0.internals.geoLockTimer.fold {
        // Сейчас не ожидаются координаты. Просто сохранить координаты в состояние карты.
        m.orig.locationOpt.fold( noChange ) { geoLoc =>
          val v2 = withMapCenter(geoLoc.point, v0)
          val isMapOpened = v0.index.search.isShownTab(MSearchTabs.GeoMap)
          val isSilentUpdate = !isMapOpened
          ah.updateMaybeSilent(isSilentUpdate)(v2)
        }

      } { geoLockTimerId =>
        // Прямо сейчас этот контроллер ожидает координаты.
        // Функция общего кода завершения ожидания координат: запустить выдачу, выключить geo loc, грохнуть таймер.
        def __finished(v00: MScRoot) = {
          val fxs = getIndexFx(geoIntoRcvr = true) + geoOffFx
          DomQuick.clearTimeout(geoLockTimerId)
          val v22 = _removeTimer(v00)
          updatedSilent(v22, fxs)
        }

        // Ожидаются координаты геолокации прямо сейчас.
        m.orig.either.fold(
          // Ожидаются координаты, но пришла ошибка. Можно ещё подождать, но пока считаем, что это конец.
          // Скорее всего, юзер отменил геолокацию или что-то ещё хуже.
          {_ =>
            __finished(v0)
          },
          // Есть какие-то координаты, но не факт, что ожидаемо точные.
          {geoLoc =>
            // Т.к. работает suppressor, то координаты можно всегда записывать в состояние, не боясь постороннего "шума".
            val v1 = withMapCenter(geoLoc.point, v0)

            if (m.orig.glType.highAccuracy) {
              // Пришли точные координаты. Завершаем ожидание.
              __finished(v1)
            } else {
              // Пока получены не точные координаты. Надо ещё подождать координат по-точнее...
              updatedSilent(v1)
            }
          }
        )
      }


    // Наступил таймаут ожидания геолокации. Нужно активировать инициализацию в имеющемся состоянии
    case GeoLocTimeOut =>
      val v0 = value
      v0.internals.geoLockTimer.fold {
        noChange
      } { _ =>
        // Удалить из состояния таймер геолокации, запустить выдачу.
        val v2 = _removeTimer(v0)
        val fxs = getIndexFx(geoIntoRcvr = true) + geoOffFx
        updatedSilent(v2, fxs)
      }

    // Если юзер активно тыкал пальцем по экрану, то таймер сокрытия мог сработать после окончания приветствия.
    case _: WcTimeOut =>
      noChange

  }

  private def _removeTimer(v0: MScRoot = value): MScRoot = {
    v0.withInternals(
      v0.internals.withGeoLockTimer( None )
    )
  }

  private def getIndexFx(geoIntoRcvr: Boolean) = Effect.action( GetIndex(withWelcome = true, geoIntoRcvr = geoIntoRcvr) )

  private def geoOffFx = Effect.action( GeoLocOnOff(enabled = false) )

  private def withMapCenter(center: MGeoPoint, v1: MScRoot = value): MScRoot = {
    v1.withIndex(
      withMapCenter(center, v1.index)
    )
  }
  private def withMapCenter(center: MGeoPoint, inx: MScIndex): MScIndex = {
    inx.withSearch(
      inx.search.withMapInit(
        inx.search.mapInit.withState(
          inx.search.mapInit.state.withCenterInitReal(
            center
          )
        )
      )
    )
  }

}
