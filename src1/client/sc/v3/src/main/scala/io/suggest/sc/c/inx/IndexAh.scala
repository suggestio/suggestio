package io.suggest.sc.c.inx

import diode._
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MLocEnv
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits.ActionHandlerExt
import io.suggest.sc.ScConstants
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m._
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search.MapReIndex
import io.suggest.sc.sc3._
import io.suggest.sc.styl.MScCssArgs
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.ScCssFactory
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits.EffectsOps
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.c.grid.GridAh
import io.suggest.sc.c.search.SearchAh
import io.suggest.sc.v.search.SearchCss
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:39
  * Description: Контроллер index'а выдачи.
  */

class IndexRespHandler( scCssFactory: ScCssFactory )
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[IScIndexRespReason]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.resp )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    val i0 = ctx.value0.index
    LOG.error( ErrorMsgs.GET_NODE_INDEX_FAILED, ex, msg = ctx.m )
    var i2 = i0.withResp(
      i0.resp.fail(ex)
    )
    if (i2.search.geo.mapInit.loader.nonEmpty) {
      i2 = i2.withSearch(
        i2.search.resetMapLoader
      )
    }
    ctx.value0.withIndex( i2 )
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.Index
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect]) = {
    val i0 = ctx.value0.index
    val inx = ra.index.get

    // Сайд-эффекты закидываются в этот аккамулятор:
    var fxsAcc = List.empty[Effect]

    // TODO Сравнивать полученный index с текущим состоянием. Может быть ничего сохранять не надо?
    var i1 = i0.copy(
      resp = i0.resp.ready(inx),
      state = i0.state
        .withRcvrNodeId( inx.nodeId.toList ),
      search = {
        var s0 = i0.search

        // Выставить полученную с сервера геоточку как текущую.
        s0 = inx.geoPoint
          .filter { mgp =>
            !(i0.search.geo.mapInit.state.center ~= mgp)
          }
          .fold(s0) { mgp =>
            s0.withGeo(
              s0.geo.withMapInit(
                s0.geo.mapInit.withState(
                  s0.geo.mapInit.state.copy(
                    centerInit = mgp,
                    centerReal = None,
                    // Увеличить зум, чтобы приблизить.
                    zoom = 15
                  )
                )
              )
            )
          }

        // Если возвращена userGeoLoc с сервера, которая запрашивалась и до сих пор она нужна, то её выставить в состояние.
        for {
          _ <- inx.userGeoLoc
          if s0.geo.mapInit.userLoc.isEmpty
        } {
          s0 = s0.withGeo(
            s0.geo.withMapInit(
              s0.geo.mapInit.withUserLoc( inx.userGeoLoc )
            )
          )
        }

        // Возможный сброс состояния тегов
        s0 = s0.maybeResetNodesFound

        // Если заход в узел с карты, то надо скрыть search-панель.
        if (s0.panel.opened && inx.welcome.nonEmpty) {
          s0 = s0.withPanel(
            s0.panel.withOpened( false )
          )
        }

        // Сбросить флаг mapInit.loader, если он выставлен.
        if (s0.geo.mapInit.loader.nonEmpty)
          s0 = s0.resetMapLoader

        s0
      }
    )

    val respActionTypes = ctx.m.tryResp.get.respActionTypes
    // Если панель поиск видна, то запустить поиск узлов в фоне.
    if (i1.search.panel.opened && !respActionTypes.contains(MScRespActionTypes.SearchNodes))
      fxsAcc ::= SearchAh.reDoSearchFx( ignorePending = false )

    // Возможно, нужно организовать обновление URL в связи с обновлением состояния узла.
    fxsAcc ::= ResetUrlRoute.toEffectPure

    // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
    if ( !respActionTypes.contains(MScRespActionTypes.AdsTile) ) {
      fxsAcc ::= GridLoadAds(clean = true, ignorePending = true)
        .toEffectPure
    }

    // Инициализация приветствия. Подготовить состояние welcome.
    val mWcSFutOpt = for {
      resp <- i1.resp.toOption
      if !i1.resp.isFailed      // Всякие FailingStale содержат старый ответ и новую ошибку, их надо отсеять.
      _    <- resp.welcome
    } yield {
      val tstamp = System.currentTimeMillis()

      // Собрать функцию для запуска неотменяемого таймера.
      // Функция возвращает фьючерс, который исполнится через ~секунду.
      val tpFx = Effect {
        WelcomeAh.timeout( ScConstants.Welcome.HIDE_TIMEOUT_MS, tstamp )
      }

      // Собрать начальное состояние приветствия:
      val mWcS = MWelcomeState(
        isHiding    = false,
        timerTstamp = tstamp
      )

      (tpFx, mWcS)
    }
    i1 = i1.withWelcome( mWcSFutOpt.map(_._2) )

    // Нужно отребилдить ScCss, но только если что-то реально изменилось.
    val scCssArgs2 = MScCssArgs.from(i1.resp, ctx.value0.dev.screen.info)
    if (scCssArgs2 != i1.scCss.args) {
      // Изменились аргументы. Пора отребилдить ScCss.
      i1 = i1.withScCss(
        scCssFactory.mkScCss( scCssArgs2 )
      )
    }

    // Объединить эффекты плитки и приветствия воедино:
    for (mwc <- mWcSFutOpt)
      fxsAcc ::= mwc._1

    val v2 = ctx.value0.withIndex( i1 )
    val fxOpt = fxsAcc.mergeEffects
    (v2, fxOpt)
  }

}


class IndexAh[M](
                  api           : IScUniApi,
                  modelRW       : ModelRW[M, MScIndex],
                  rootRO        : ModelRO[MScRoot],
                  scCssFactory  : ScCssFactory,
                )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Непосредственный запуск получения индекса с сервера.
    *
    * @param withWelcome Надо ли серверу готовить и возвращать welcome-данные.
    * @param silentUpdate Не рендерить на экране изменений?
    * @param v0 Исходныое значение MScIndex.
    * @param geoIntoRcvr Допускать geo-детектирование ресивера.
    * @param reason Экшен-причина, приведший к запуску запроса.
    * @return ActionResult.
    */
  private def _getIndex(withWelcome: Boolean, silentUpdate: Boolean, v0: MScIndex,
                        geoIntoRcvr: Boolean, reason: IScIndexRespReason, retUserLoc: Boolean): ActionResult[M] = {
    val ts = System.currentTimeMillis()
    val root = rootRO.value

    val isSearchNodes = root.index.search.panel.opened
    val searchArgs = MAdsSearchReq(
      limit  = Some( GridAh.adsPerReqLimit ),
      genOpt = Some( root.index.state.generation ),
      offset = Some( 0 ),
      textQuery = OptionUtil.maybeOpt(isSearchNodes) {
        Option( root.index.search.text.query )
          .filter(_.nonEmpty)
      }
    )

    val fx = Effect {
      val someTrue = Some( true )
      //println("get index @" + System.currentTimeMillis())
      val args = MScQs(
        common = MScCommonQs(
          apiVsn = root.internals.conf.apiVsn,
          // Когда уже задан id-ресивера, не надо слать на сервер маячки и географию.
          locEnv = v0.state.currRcvrId
            .fold(root.locEnv)(_ => MLocEnv.empty),
          screen = Some( root.dev.screen.info.screen ),
          searchGridAds = someTrue,
          // Сразу запросить поиск по узлам, если панель поиска открыта.
          searchNodes = if (isSearchNodes) someTrue else None
        ),
        index = Some {
          MScIndexArgs(
            nodeId      = v0.state.currRcvrId,
            withWelcome = withWelcome,
            geoIntoRcvr = geoIntoRcvr,
            retUserLoc  = retUserLoc
          )
        },
        // Фокусироваться надо при запуске. Для этого следует получать всё из reason, а не из состояния.
        foc = for {
          focAdId <- reason.focusedAdId
        } yield {
          MScFocusArgs(
            focIndexAllowed = true,
            lookupMode      = None,
            lookupAdId      = focAdId
          )
        },
        search = searchArgs
      )

      api
        .pubApi(args)
        .transform { tryRes =>
          val r2 = HandleScApiResp(
            reqTimeStamp  = Some(ts),
            qs            = args,
            tryResp       = tryRes,
            reason        = reason
          )
          Success(r2)
        }
    }

    var v2 = v0.withResp(
      v0.resp.pending(ts)
    )

    // Выставить в состояние, что запущен поиск узлов, чтобы не было дублирующихся запросов от контроллера панели.
    if (isSearchNodes) {
      v2 = v2.withSearch(
        v2.search.withGeo(
          v2.search.geo.withFound(
            v2.search.geo.found.withReqWithArgs(
              req = v2.search.geo.found.req.pending(ts),
              reqSearchArgs = Some(searchArgs)
            )
          )
        )
      )
    }

    ah.updateMaybeSilentFx(silentUpdate)(v2, fx)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен управления отображением боковых панелей выдачи.
    case m: SideBarOpenClose =>
      val v0 = value
      m.bar match {
        case MScSideBars.Search =>
          if (v0.search.panel.opened ==* m.open) {
            // Ничего делать не надо - ничего не изменилось.
            noChange

          } else {
            // Действительно изменилось состояние отображения панели:
            var v2 = v0.withSearch(
              v0.search.withPanel(
                v0.search.panel
                  .withOpened( m.open )
              )
            )

            // Не допускать открытости обеих панелей одновременно:
            if (m.open && v2.menu.opened) {
              v2 = v2.withMenu(
                v2.menu.withOpened(false)
              )
            }

            // Аккаумулятор сайд-эффектов.
            val routeFx = ResetUrlRoute.toEffectPure

            // Требуется ли запускать инициализацию карты или списка найденных узлов? Да, если открытие на НЕинициализированной панели.
            val fxOpt = OptionUtil.maybeOpt(m.open) {
              SearchAh.maybeInitSearchPanel(v2.search)
            }

            // Объеденить эффекты:
            val finalFx = (routeFx :: fxOpt.toList)
              .mergeEffects
              .get

            updated(v2, finalFx)
          }

        case MScSideBars.Menu =>
          if (v0.menu.opened !=* m.open) {
            var v2 = v0.withMenu(
              v0.menu.withOpened( m.open )
            )
            // Обновить URL.
            val fx = ResetUrlRoute.toEffectPure

            // Не допускать открытости обоих панелей одновременно.
            if (m.open && v2.search.panel.opened) {
              v2 = v2.withSearch(
                v2.search.withPanel(
                  v2.search.panel
                    .withOpened( false )
                )
              )
            }

            updated( v2, fx )
          } else {
            noChange
          }
      }


    // Кто-то затребовал перерендерить css-стили выдачи. Скорее всего, размеры экрана изменились.
    case ScCssReBuild =>
      val v0 = value
      val scCssArgs = MScRoot.scCssArgsFrom( rootRO.value )
      if (v0.scCss.args != scCssArgs) {
        val scCss2 = scCssFactory.mkScCss( scCssArgs )
        val searchCss2 = SearchCss( v0.search.geo.css.args.withScreenInfo(scCssArgs.screenInfo) )
        val v2 = v0
          .withScCss( scCss2 )
          .withSearch(
            v0.search.withGeo(
              v0.search.geo
                .withCss( searchCss2 )
            )
          )
        val rszMapFx = for (lmap <- v0.search.geo.data.lmap) yield SearchAh.mapResizeFx(lmap)
        ah.updatedMaybeEffect(v2, rszMapFx)
      } else {
        noChange
      }


    // Необходимо перезапросить индекс для текущего состояния карты
    case m: MapReIndex =>
      val v0 = value
      if (
        (m.rcvrId.nonEmpty && m.rcvrId ==* v0.state.currRcvrId) ||
        (m.rcvrId.isEmpty && v0.search.geo.mapInit.state.isCenterRealNearInit)
      ) {
        // Ничего как бы и не изменилось.
        noChange
      } else {
        // Выставить новое состояние и запустить GetIndex.
        val v1 = v0
          .withState(
            v0.state
              .withRcvrNodeId( m.rcvrId.toList )
          )
          .withSearch(
            v0.search.withGeo(
              v0.search.geo.withMapInit(
                v0.search.geo.mapInit.withLoader(
                  Some( v0.search.geo.mapInit.state.center )
                )
              )
            )
          )

        _getIndex(
          withWelcome   = m.rcvrId.nonEmpty,
          silentUpdate  = false,
          v0            = v1,
          geoIntoRcvr   = m.rcvrId.nonEmpty,
          reason        = m,
          retUserLoc    = false
        )
      }


    // Команда запроса и получения индекса выдачи с сервера для текущего состояния.
    case m: GetIndex =>
      _getIndex(
        withWelcome   = m.withWelcome,
        silentUpdate  = true,
        v0            = value,
        geoIntoRcvr   = m.geoIntoRcvr,
        reason        = m,
        retUserLoc    = m.retUserLoc
      )

  }

}

