package io.suggest.sc.c.inx

import diode._
import diode.data.Pot
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits.ActionHandlerExt
import io.suggest.sc.ScConstants
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m._
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search.{DoSearch, MapReIndex}
import io.suggest.sc.sc3._
import io.suggest.sc.search.MSearchTabs
import io.suggest.sc.styl.MScCssArgs
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.ScCssFactory
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits.EffectsOps
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.c.grid.GridAh
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
                    centerReal = None
                    // TODO выставлять ли zoom?
                  )
                )
              )
            )
          }

        // Возможный сброс состояния тегов
        s0 = s0.maybeResetTags

        // Если заход в узел с карты, то надо скрыть search-панель.
        if (s0.isShown && inx.welcome.nonEmpty)
          s0 = s0.withIsShown( false )

        // Сбросить флаг mapInit.loader, если он выставлен.
        if (s0.geo.mapInit.loader.nonEmpty)
          s0 = s0.resetMapLoader

        // 2018-03-23 Решено, что внутри узлов надо открывать сразу теги, ибо каталог.
        // А на карте - в первую очередь открывать карту.
        // Поэтому принудительно меняем текущую search-вкладку:
        val nextSearchTab = MSearchTabs.defaultIfRcvr( inx.isRcvr )
        if (nextSearchTab !=* s0.currTab)
        // Надо сменить search-таб согласно режиму текущего возможного узла.
          s0 = s0.withCurrTab(nextSearchTab)

        s0
      }
    )

    val respActionTypes = ctx.m.tryResp.get.respActionTypes
    // Если вкладка с тегами видна, то запустить получение тегов в фоне.
    if (i1.search.isShownTab(MSearchTabs.Tags) && !respActionTypes.contains(MScRespActionTypes.SearchRes)) {
      fxsAcc ::= Effect.action {
        DoSearch(clear = true)
      }
    }

    // Возможно, нужно организовать обновление URL в связи с обновлением состояния узла.
    fxsAcc ::= Effect.action( ResetUrlRoute )

    // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
    if ( !respActionTypes.contains(MScRespActionTypes.AdsTile) ) {
      fxsAcc ::= Effect.action {
        GridLoadAds(clean = true, ignorePending = true)
      }
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
        WelcomeUtil.timeout( ScConstants.Welcome.HIDE_TIMEOUT_MS, tstamp )
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
    val fxOpt = fxsAcc.mergeEffectsSet
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
                        geoIntoRcvr: Boolean, reason: IScIndexRespReason): ActionResult[M] = {
    val ts = System.currentTimeMillis()

    val fx = Effect {
      val root = rootRO.value

      //println("get index @" + System.currentTimeMillis())
      val args = MScQs(
        common = MScCommonQs(
          apiVsn = root.internals.conf.apiVsn,
          locEnv = root.locEnv,
          screen = Some( root.dev.screen.info.screen ),
          searchGridAds = Some( true )
        ),
        index = Some(
          MScIndexArgs(
            nodeId      = v0.state.currRcvrId,
            withWelcome = withWelcome,
            geoIntoRcvr = geoIntoRcvr
          )
        ),
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
        search = MAdsSearchReq(
          limit  = Some( GridAh.adsPerReqLimit ),
          genOpt = Some( root.index.state.generation ),
          offset = Some( 0 )
        )
      )

      api
        .pubApi(args)
        .transform { tryRes =>
          val r2 = HandleScApiResp(
            reqTimeStamp  = Some(ts),
            apiReq        = args,
            tryResp       = tryRes,
            reason        = reason
          )
          Success(r2)
        }
    }

    val v2 = v0.withResp(
      v0.resp.pending(ts)
    )

    ah.updateMaybeSilentFx(silentUpdate)(v2, fx)
  }


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Кто-то затребовал перерендерить css-стили выдачи. Скорее всего, размеры экрана изменились.
    case ScCssReBuild =>
      val v0 = value
      val scCssArgs = MScRoot.scCssArgsFrom( rootRO.value )
      if (v0.scCss.args != scCssArgs) {
        val scCss2 = scCssFactory.mkScCss( scCssArgs )
        val v2 = v0.withScCss( scCss2 )
        updated(v2)
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
          reason        = m
        )
      }


    // Команда запроса и получения индекса выдачи с сервера для текущего состояния.
    case m: GetIndex =>
      _getIndex(
        withWelcome   = m.withWelcome,
        silentUpdate  = true,
        v0            = value,
        geoIntoRcvr   = m.geoIntoRcvr,
        reason        = m
      )

  }

}

