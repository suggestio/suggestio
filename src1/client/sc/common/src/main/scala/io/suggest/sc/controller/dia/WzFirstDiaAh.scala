package io.suggest.sc.controller.dia

import cordova.Cordova
import diode._
import diode.Implicits._
import diode.data.{Pot, Ready}
import io.suggest.radio.beacon.{BtOnOff, IBeaconsListenerApi, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.conf.ConfConst
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.{MOsFamilies, MOsFamily, MPlatformS}
import io.suggest.geo.GeoLocUtilJs
import io.suggest.msg.ErrorMsgs
import io.suggest.os.notify.NotificationPermAsk
import io.suggest.os.notify.api.cnl.CordovaNotificationlLocalUtil
import io.suggest.os.notify.api.html5.Html5NotificationUtil
import io.suggest.perm.{BoolOptPermissionState, CordovaDiagonsticPermissionUtil, Html5PermissionApi, IPermissionState}
import io.suggest.sc.model.{GeoLocOnOff, GeoLocTimerStart, MScRoot, ResetUrlRoute, SettingAction, SettingSet}
import io.suggest.sc.model.dia.first._
import io.suggest.log.Log
import io.suggest.sc.ScCommonCircuit
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.model.inx.MScSwitchCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{CircuitUtil, DoNothing}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.concurrent.{Future, Promise, TimeoutException}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import io.suggest.sc.util.Sc3ConfUtil
import org.scalajs.dom.experimental.permissions.PermissionName
import play.api.libs.json.JsBoolean

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.19 11:47
  * Description: Контроллер для визардов.
  * Обновлённая логика работы такова:
  * - Инициализация:
  *   - Синхронная проверка доступа
  *   - Опциональный скрытый рендер диалога + запуск фоновых проверок доступа.
  * - Если нет доступа к чему-либо, то запросить доступ.
  * - Сохранить в localStorage, что инициализация уже была проведена.
  */
class WzFirstDiaAh[M <: AnyRef](
                                 platformRO       : ModelRO[MPlatformS],
                                 modelRW          : ModelRW[M, MWzFirstOuterS],
                                 sc3Circuit       : ScCommonCircuit,
                               )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  // Быстрое выставление фрейма.
  private def _setFrame(frame: MWzFrame, v0: MWzFirstOuterS, view0: MWzFirstS) = {
    val view2 = (MWzFirstS.frame replace frame)(view0)
    (MWzFirstOuterS.view replace Ready( view2 ).pending())(v0)
  }

  /** Make Effect of updating phase-related settings. */
  private def _emitSettingFx(phaseSpec: IPermissionSpec, isGranted: Boolean): Option[Effect] = {
    val settingsActions = phaseSpec.settings( isGranted )
    OptionUtil.maybeOpt( settingsActions.nonEmpty ) {
      (for {
        settingsAction <- settingsActions.iterator
      } yield {
        settingsAction.toEffectPure
      })
        .mergeEffects
    }
  }


  private def _doPermissionRequest(v0: MWzFirstOuterS, view0: MWzFirstS, phaseSpec: IPermissionSpec): (MWzFirstOuterS, Effect) = {
    val permStatePot = v0.perms.getOrElse( view0.phase, Pot.empty )
    val permStateOpt = permStatePot.toOption
    val someStartTimeMs = Some( System.currentTimeMillis() )

    val hasOnChangeApi = permStateOpt.exists(_.hasOnChangeApi)
    val timeoutMs = (if (hasOnChangeApi) 10 else 4) * 1000

    // Close InProgress-dialog via timeout:
    var fxAcc: Effect = Effect {
      _permissionTimeout( view0.phase, timeoutMs, someStartTimeMs )
        .fut
    }

    // Subscribe for permission state changes, if possible:
    if (hasOnChangeApi) for (permState <- permStateOpt) {
      fxAcc += Effect.action {
        // Subscribe for permission state changes:
        permState.onChange { pss: IPermissionState =>
          val action = WzPhasePermRes( view0.phase, Success(pss), startTimeMs = someStartTimeMs )
          sc3Circuit.dispatch( action )
        }
        DoNothing
      }
    }

    // Subscribe for changes via some Pot inside Sc state:
    for (onChangePotZoom <- phaseSpec.listenChangesOfPot) {
      fxAcc += Effect {
        CircuitUtil
          .promiseSubscribe()
          .withTimeout( timeoutMs )   // TODO Two timeout here created at once with same millis interval. Replace with timer future completeness.
          .zooming( sc3Circuit, onChangePotZoom )
          .transform { case tryRes =>
            val permResMapped = for (_ <- tryRes) yield {
              BoolOptPermissionState( OptionUtil.SomeBool.someTrue )
            }
            val action = WzPhasePermRes( phaseSpec.phase, permResMapped, startTimeMs = someStartTimeMs )
            Success(action)
          }
      }
    }

    fxAcc += phaseSpec.requestPermissionFx

    // Переключить view в состояние ожидания.
    val v2 = MWzFirstOuterS.perms.modify {
      // set pending state:
      _.updated( phaseSpec.phase, permStatePot.pending( someStartTimeMs.value ) )
    }( _setFrame( MWzFrames.InProgress, v0, view0 ) )

    (v2, fxAcc)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопкам в диалоге.
    case m: YesNoWz =>
      val v0 = value
      v0.view.fold(noChange) { view0 =>
        if (m.yesNo) {
          // Положительный ответ - запустить запрос разрешения.
          view0.frame match {
            case MWzFrames.AskPerm =>
              // Положительный ответ - запросить доступ в реальности.
              (for {
                phaseSpec <- _findPhaseSpec( view0.phase )
              } yield {
                val (v2, fx) = _doPermissionRequest( v0, view0, phaseSpec )
                updated( v2, fx )
              })
                .getOrElse {
                  // Нет фонового запроса доступа - нечего ожидать. Хотя этой ситуации быть не должно.
                  _wzGoToNextPhase( v0 )
                }

            // yes в info-окне означает retry, по идее.
            case MWzFrames.Info =>
              val v2 = _setFrame( MWzFrames.AskPerm, v0, view0 )
              updated( v2 )

            // yes во время InProgress. Недопустимо, ожидание можно только отменить.
            case other @ MWzFrames.InProgress =>
              logger.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (other, m) )
              noChange
          }

        } else {
          view0.frame match {
            // Закрытие окошка с инфой - переход на след.фазу
            case MWzFrames.Info =>
              _wzGoToNextPhase( v0 )
            // Осознанный отказ в размещении - перейти в Info-фрейм текущей фазы
            case MWzFrames.AskPerm =>
              if (WzFirstDiaAh.CRY_ABOUT_REFUSE_LATER) {
                val v2 = _setFrame(MWzFrames.Info, v0, view0 )
                updated(v2)
              } else {
                _wzGoToNextPhase( v0 )
              }
            // false во время InProgress - отмена ожидания. Надо назад перебросить, на Ask-шаг.
            case MWzFrames.InProgress =>
              val v2 = _setFrame( MWzFrames.AskPerm, v0, view0 )
              // Отменить ожидание результата пермишена для текущей фазы:
              // TODO Унести управление onChange за пределы YesNo-сигнала.
              val fx = _wzUnWatchPermChangesFx( v0 )
              updated(v2, fx)
          }
        }

      }


    // Action with async permission state reading result.
    case m: WzPhasePermRes =>
      val v0 = value

      // Save received permission info to current state:
      lazy val permOpt0 = v0.perms.get( m.phase )
      if (
        m.res.isSuccess || permOpt0.fold(true) { permPot0 =>
          m.startTimeMs.fold( permPot0.isPending )( permPot0.isPendingWithStartTime )
        }
      ) {
        val permStatePot2 = permOpt0
          .getOrElse( Pot.empty )
          .withTry( m.res )
        val v1 = MWzFirstOuterS.perms.modify( _.updated( m.phase, permStatePot2 ) )(v0)

        var fxAcc = List.empty[Effect]

        // Additional effects for all branches, if any:
        for {
          reason <- m.reason
          onCompleteFx <- reason.onComplete
          stillHavePendingPerm = v1.perms
            .valuesIterator
            .exists(_.isPending)
          if !stillHavePendingPerm
        } {
          // No more pending permissions. Do onComplete effect, if any.
          fxAcc ::= onCompleteFx
        }

        for {
          phaseSpec <- _findPhaseSpec( m.phase )
        } {
          // Update settings, if permission explicitly granted or denied.
          for {
            permState2  <- permStatePot2
            isGranted   = permState2.isGranted
            if isGranted
            fx <- _emitSettingFx( phaseSpec, isGranted )
          } {
            fxAcc ::= fx
          }
        }

        val fxOpt = fxAcc.mergeEffects

        // Is wizard visible or background activities?
        (for {
          view0 <- v1.view
        } yield {
          // Надо понять, сейчас текущая фаза или какая-то другая уже. Всякое бывает.
          if (m.phase ==* view0.phase) {
            // Это текущая фаза.
            val isGrantedOpt =
              for (permState <- m.res.toOption)
              yield permState.isGranted

            view0.frame match {

              // Сейчас происходит ожидание ответа юзера в текущей фазе. Всё по плану. Но по плану ли ответ?
              case MWzFrames.InProgress =>
                if (isGrantedOpt contains[Boolean] false) {
                  // Юзер не разрешил. Вывести Info с сожалением.
                  val v2 = MWzFirstOuterS.view.replace( Ready(
                    MWzFirstS.frame.replace( MWzFrames.Info )(view0)
                  ).pending())(v1)
                  ah.updatedMaybeEffect( v2, fxOpt )
                } else {
                  // Положительный результат или отсутствие ответа. Просто перейти на следующую фазу:
                  _wzGoToNextPhase( v1, fxOpt )
                }

              // Ответ по timeout
              case _ if m.res.isFailure =>
                ah.updatedSilentMaybeEffect( v1, fxOpt )

              // Ответ от юзера - является ценным.
              case MWzFrames.Info =>
                if (isGrantedOpt contains[Boolean] true) {
                  // Положительный ответ + Info => следующая фаза.
                  _wzGoToNextPhase( v1, fxOpt )
                } else {
                  ah.updatedSilentMaybeEffect( v1, fxOpt )
                }

              // Ответ юзера наступил во время вопроса текущей фазы.
              // Возможно, если разрешение было реально запрошено ещё какой-то подсистемой выдачи (за пределами этого диалога), косяк.
              case ph @ MWzFrames.AskPerm =>
                logger.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (ph, m) )
                if (isGrantedOpt contains[Boolean] true) {
                  _wzGoToNextPhase( v1, fxOpt )
                } else {
                  val v2 = _setFrame( MWzFrames.Info, v1, view0 )
                  ah.updatedMaybeEffect( v2, fxOpt )
                }

            }

          } else if (view0.phase ==* MWzPhases.Starting) {
            // Надо показать на экране текущий диалог в разном состоянии.
            // TODO Opt hasPending should be false, if NEXT phase is ready to display to user.
            val hasPending = v1.perms
              .valuesIterator
              .exists(_.isPending)

            if (hasPending) {
              ah.updatedSilentMaybeEffect( v1, fxOpt )
            } else {
              // Больше нет pending-задач. Переключиться на следующую фазу диалога.
              _wzGoToNextPhase( v1, fxOpt )
            }

          } else {
            // Пока разрешения разруливались, фаза уже изменилась неизвестно куда. Не ясно, возможно ли такое на яву.
            // Пока просто молча пережёвываем.
            logger.log( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (m, view0.phase) )
            ah.updatedSilentMaybeEffect( v1, fxOpt )
          }
        })
          .getOrElse {
            // Dialog not opened. This is a background testing. Just process state with possible effects.
            ah.updatedSilentMaybeEffect( v1, fxOpt )
          }

      } else {
        // Refusing to update permission state. Logging only something useful:
        if (
          //scalajs.LinkingInfo.developmentMode ||
          !m.res.failed.toOption.exists(_.isInstanceOf[TimeoutException])
        )
          logger.log( ErrorMsgs.SUPPRESSED_INSUFFICIENT, msg = (m, permOpt0.orNull) )

        noChange
      }


    // Start for dumping current permissions states into wizard state.
    case m: WzReadPermissions =>
      val v0 = value

      // Запускаемся.
      var fxsAcc: List[Effect] = Nil
      var permPotsAcc = List.empty[((MWzPhase, Pot[IPermissionState]), Long)]

      val someReason = Some(m)
      val someStartTimeMs = Some( System.currentTimeMillis() )
      // Пройтись по списку фаз, активировав проверки прав:
      for {
        phaseSpec <- _allPhaseSpecs
        if {
          ( // Filter by only allowed phases, if filtering enabled.
            m.onlyPhases.isEmpty ||
            (m.onlyPhases contains[MWzPhase] phaseSpec.phase)
          ) &&
            phaseSpec.isSupported()
        }

        permPot0 = v0.perms.getOrElse( phaseSpec.phase, Pot.empty[IPermissionState] )
        // Prevent double-check until timeout: underlying native API may suffer from too simulateos calls.
        if !permPot0.isPending
      } {
        val timeoutP = _permissionTimeout( phaseSpec.phase, timeoutMs = 3000, startTimeMs = someStartTimeMs )
        val phasePermFx: Effect =
          _readPermissionFx( phaseSpec, reason = someReason, someStartTimeMs ) +
          Effect( timeoutP.fut )
        fxsAcc ::= phasePermFx

        // Update current permission Pot[] state to pending:
        val permPot2 = permPot0
          .pending( someStartTimeMs.value )
        permPotsAcc ::= ((phaseSpec.phase -> permPot2) -> someStartTimeMs.value)
      }

      // Start single timer with many actions for all pending tasks:
      (for {
        ((phase, _), startTimeMs) <- permPotsAcc.iterator
      } yield {
        Effect.action( WzPhasePermRes( phase, Failure(new TimeoutException), someReason, Some(startTimeMs) ) )
      })
        .mergeEffects
        .foreach { fx =>
          fxsAcc ::= fx.after( 3.seconds )
        }

      val v2 = MWzFirstOuterS.perms.modify( _ ++ permPotsAcc.map(_._1) )(v0)
      ah.updatedSilentMaybeEffect( v2, fxsAcc.mergeEffects )


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      val v0 = value
      if (
        m.showHide &&
        v0.view.isEmpty &&
        WzFirstDiaAh.isNeedWizardFlowVal
      ) {
        val (phases, reason) = if (m.onlyPhases.isEmpty) {
          val phases2 =_platformStartPhases()
          phases2 -> m.copy(onlyPhases = phases2)
        } else {
          m.onlyPhases -> m
        }
        val readPermsFx = Effect.action {
          WzReadPermissions(
            onlyPhases = phases,
          )
        }
        // TODO Insert into URL info about running wz? This is non-actual anymore?
        val resetUrlRoute = ResetUrlRoute(force = true).toEffectPure

        // Инициализировать состояние first-диалога.
        val v2 = (
          MWzFirstOuterS.view replace Ready(MWzFirstS(
            phase = MWzPhases.Starting,
            frame = MWzFrames.InProgress,
            reason = reason,
          )).pending()
        )(v0)

        val fx = readPermsFx >> resetUrlRoute
        updated( v2, fx )

      } else if (!m.showHide) {
        var fxAcc = List.empty[Effect]

        // All completion effects - to acc:
        for {
          onCompleteFxOpt <- (
            m.onComplete ::
            v0.view
              .toOption
              .flatMap(_.reason.onComplete) ::
            Nil
          )
          onCompleteFx <- onCompleteFxOpt
        } {
          fxAcc ::= onCompleteFx
        }

        // TODO Maybe delete this effect after LocateButton integration.
        // Ensure geolocation timer on first run after permissions are processed, etc:
        // TODO XXX GeoLoc Permission request on android always fails with TimeoutException! Also, hasLocationAccess here - fails.
        val hasLocAccess = v0.perms.hasLocationAccess
        if (hasLocAccess) {
          fxAcc ::= Effect.action {
            GeoLocTimerStart(
              MScSwitchCtx(
                demandLocTest = true,
                indexQsArgs = MScIndexArgs(
                  geoIntoRcvr = true,
                  returnEphemeral = true,
                ),
                /*forceGeoLoc = sc3Circuit.scGeoLocRW
                  .value
                  .currentLocation
                  .map(_._2),*/
              ),
              allowImmediate = false,
            )
          }
        }

        // Closing opened first-run snack-dialog:
        val v2Opt = OptionUtil.maybe( !v0.view.isUnavailable ) {
          MWzFirstOuterS.view.modify(_.unavailable())(v0)
        }

        ah.optionalResult( v2Opt, fxAcc.mergeEffects, silent = false )

      } else if (v0.view ==* Pot.empty) {
        // Forbid to start and write this decision into state:
        val v2 = MWzFirstOuterS.view.modify(_.unavailable())(v0)
        val fx = ResetUrlRoute().toEffectPure
        // Non-silent, so BootAh can subscribe/listen for Pot changes here.
        updated(v2, fx)

      } else {
        logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      }


    case m: WzDebugView =>
      val v0 = value
      val view2 = MWzFirstS(
        phase = m.phase,
        frame = m.frame,
        reason = v0.view
          .fold( InitFirstRunWz(
            showHide = true,
            onlyPhases = m.phase :: Nil,
          ))(_.reason),
      )
      val v2 = MWzFirstOuterS.view.modify( _.ready( view2 ) )(v0)
      updated(v2)

  }


  /** Перещёлкивание на следующую фазу диалога. */
  private def _wzGoToNextPhase(v0: MWzFirstOuterS, fxOpt: Option[Effect] = None): ActionResult[M] = {
    val view0 = v0.view.get
    var fxAcc = fxOpt.toList

    (for {
      nextPhase <- MWzPhases
        .values
        // Обязательно iterator, т.к. тут нужна лень и ТОЛЬКО первый успешный результат.
        .iterator
        // Получить итератор фаз ПОСЛЕ текущей фазы:
        .dropWhile { nextPhase =>
          nextPhase !=* view0.phase
        }
        .drop(1)

      if v0.view.fold(true) { v =>
        v.reason.onlyPhases.isEmpty ||
        (v.reason.onlyPhases contains[MWzPhase] nextPhase)
      }

      // Найти и разобраться с pot с пермишшеном фазы:
      permPot <- v0.perms.get( nextPhase )

      // Основная логика упакована сюда:
      v9 <- permPot
        // По идее, тут или ready, или failed.
        .toOption
        .flatMap[MWzFirstOuterS] { perm =>
          // avoiding perm.isPrompt, because unsure about cases like https://github.com/dpa99c/cordova-diagnostic-plugin/issues/439
          val isDenied = perm.isDenied
          val isGranted = perm.isGranted

          if (!isDenied && !isGranted) {
            // Permission not yet requested. Ask user about enabling feature (or don't ask, if noAsk=true)
            val v22 = if (view0.reason.noAsk) {
              val (v2, requestPermFx) = _doPermissionRequest( v0, view0, _findPhaseSpec(nextPhase).get )
              fxAcc ::= requestPermFx
              v2
            } else {
              MWzFirstOuterS.view.replace {
                Ready(
                  view0.copy(
                    phase = nextPhase,
                    frame = MWzFrames.AskPerm
                  )
                ).pending()
              }(v0)
            }
            Some( v22 )

          } else if (isDenied) {
            // Запрещён доступ. Значит юзеру можно выразить сожаление в инфо-окне.
            val v2 = MWzFirstOuterS.view.replace {
              Ready(
                view0.copy(
                  phase   = nextPhase,
                  frame   = MWzFrames.Info
                )
              ).pending()
            }(v0)
            Some(v2)

          } else {
            // granted или что-то неведомое - пропуск фазы или завершение, если не осталось больше фаз для обработки.
            if (isGranted && nextPhase !=* view0.phase) {
              for {
                permSpec <- _findPhaseSpec( nextPhase )
                fx <- (
                  permSpec.onGrantedByDefault ::
                  _emitSettingFx(permSpec, isGranted = true) ::
                  Nil
                )
                  .iterator
                  .flatten
                  .reduceOption(_ + _)
              } {
                fxAcc ::= fx
              }
            }

            OptionUtil.maybe(v0.perms.exists(_._2.isPending))( v0 )
          }
        }
        .orElse[MWzFirstOuterS] {
          for (ex <- permPot.exceptionOption)
            logger.log( ErrorMsgs.PERMISSION_API_FAILED, ex, nextPhase )

          OptionUtil.maybe( permPot.isPending || permPot.isEmpty ) {
            // pending|empty - тут быть не должно, т.к. код вызывается после всех проверок.
            logger.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = permPot )
            v0
          }
        }
    } yield {
      // Следующая фаза одобрена:
      val fxOpt = fxAcc.mergeEffects
      ah.updatedMaybeEffect( v9, fxOpt )
    })
      .nextOption()
      .getOrElse {
        // Нет больше фаз для переключения - значит пора на выход.
        val unsubscribeChangesFx = Effect.action {
          for {
            permPot <- v0.perms.valuesIterator
            perm    <- permPot.iterator
            if perm.hasOnChangeApi
          }
            Try( perm.onChangeReset() )

          DoNothing
        }

        val saveFx = Effect.action {
          MFirstRunStored.save(
            MFirstRunStored(
              version = MFirstRunStored.Versions.CURRENT
            )
          )

          DoNothing
        }

        // Удаление из DOM
        val unRenderFx = InitFirstRunWz(false).toEffectPure

        val allFx = (saveFx :: unsubscribeChangesFx :: unRenderFx :: fxAcc)
          .mergeEffects
          .get
        // Usually, here is updated value received. So, store new state value silently.
        updatedSilent( v0, allFx )
      }
  }


  /** Сброс мониторинга изменений права доступа. */
  private def _wzUnWatchPermChangesFx(first0: MWzFirstOuterS): Effect = {
    Effect.action {
      for {
        view0   <- first0.view
        permPot <- first0.perms.get( view0.phase )
      }
        for (perm <- permPot if perm.hasOnChangeApi)
          Try(perm.onChangeReset())
      DoNothing
    }
  }

  private def _readPermissionFx( phaseSpec: IPermissionSpec, reason: Option[WzReadPermissions], startTimeMs: Some[Long] ): Effect = {
    Effect {
      phaseSpec
        .readPermissionState()
        .transform { tryPermState =>
          val action = WzPhasePermRes( phaseSpec.phase, tryPermState, reason, startTimeMs = startTimeMs )
          Success( action )
        }
    }
  }

  /** Start timer with permission phase error. */
  private def _permissionTimeout(phase: MWzPhase, timeoutMs: Double, startTimeMs: Some[Long] ) = {
    DomQuick
      .timeoutPromiseT( timeoutMs )(
        WzPhasePermRes( phase, Failure(new TimeoutException( ErrorMsgs.PERMISSION_REQUEST_TIMEOUT )), startTimeMs = startTimeMs )
      )
  }

  // Список спецификаций фаз с инструкциями, которые необходимо пройти для инициализации.
  private lazy val h5PermApiAvail = Html5PermissionApi.isApiAvail()

  /** GeoLocation access permission spec. */
  final class GeoLocationSpec extends IPermissionSpec {
    override def phase = MWzPhases.GeoLocPerm
    override def isSupported(): Boolean = {
      // Для cordova нет смысла проверять наличие плагина, который всегда должен быть.
      CordovaConstants.isCordovaPlatform() || GeoLocUtilJs.envHasGeoLoc()
    }
    override def readPermissionState(): Future[IPermissionState] = {
      def htmlAskPermF = {
        IPermissionState.maybeKnownF(h5PermApiAvail)(
          Html5PermissionApi.getPermissionState( PermissionName.geolocation )
        )
      }
      if (platformRO.value.isCordova) {
        CordovaDiagonsticPermissionUtil
          .getGeoLocPerm()
          .recoverWith { case ex: Throwable =>
            // diag-плагин на разных платформах работает по-разному. Отрабатываем ситуацию, когда он может не работать:
            logger.info( ErrorMsgs.DIAGNOSTICS_RETRIEVE_FAIL, ex, (Try(Cordova), Try(Cordova.plugins), Try(Cordova.plugins.diagnostic)) )
            htmlAskPermF
          }
      } else {
        htmlAskPermF
      }
    }
    override def requestPermissionFx = Effect.action {
      GeoLocOnOff( enabled = true, isHard = true )
    }
    override def listenChangesOfPot = Some {
      sc3Circuit.scGeoLocRW.zoom[Pot[_]] { scGeoLoc =>
        (for {
          glWatcher <- scGeoLoc.watchers.valuesIterator
          // glWatcher.watchId - not ok here, because it can be Ready() even with missing permissions.
          pot = glWatcher.lastPos
          // TODO Commented code, because watchers map have lenght 0 or 1. For many items, need to uncomment.
          //if pot !=* Pot.empty
        } yield {
          pot
        })
          .nextOption()
          .getOrElse( Pot.empty )
      }
    }
    override def settings(isGranted: Boolean): List[SettingSet] = {
      val isGrantedJs = JsBoolean( isGranted )

      // On android, location access also grants access to bluetooth scanning:
      val confKeysAcc: List[SettingSet] = {
        if (isGranted && (platformRO.value.osFamily contains[MOsFamily] MOsFamilies.Android)) {
          ConfConst.ScSettings.bluetoothKeys
            .map( SettingSet(_, isGrantedJs, save = true, runSideEffect = true ) )
        } else {
          Nil
        }
      }

      SettingSet( ConfConst.ScSettings.LOCATION_ENABLED, isGrantedJs, save = true, runSideEffect = false ) ::
        confKeysAcc
    }
  }

  /** BlueTooth scan permission specification. */
  final class BlueToothSpec extends IPermissionSpec {
    // TODO Need onChange handler, at least for android for LOCATION(!) permission.
    override def phase = MWzPhases.BlueToothPerm
    override def isSupported() = platformRO.value.hasReadioBeacons
    override def readPermissionState() = {
      val plat = platformRO.value
      plat.osFamily
        .filter(_ => plat.isCordova)
        .fold [Future[IPermissionState]] {
          Future failed new UnsupportedOperationException
        } { osFamily =>
          CordovaDiagonsticPermissionUtil.getBlueToothPermissionState(
            osFamily,
            configFut = {
              // Read current bluetooth value from configuration
              // Used for iOS-cases, because iOS requests permission when reading permission via API.
              // TODO Make this code shorten and cleaner!
              val p = Promise[Option[Boolean]]()
              val confKey = ConfConst.ScSettings.BLUETOOTH_BEACONS_ENABLED
              try {
                sc3Circuit.dispatch {
                  SettingAction(
                    key = confKey,
                    fx = { jsValue =>
                      val r = jsValue.asOpt[Boolean]
                      val fx = Effect.action {
                        p.success(r)
                        DoNothing
                      }
                      Some(fx)
                    }
                  )
                }
              } catch {
                case ex: Throwable =>
                  logger.error( ErrorMsgs.CONFIG_ACTION_FAILED, ex, (osFamily, confKey) )
                  p.tryFailure(ex)
              }
              p.future
            }
          )
        }
    }
    override def requestPermissionFx = Effect.action {
      BtOnOff(
        isEnabled = OptionUtil.SomeBool.someTrue,
        opts = MBeaconerOpts(
          scanMode    = IBeaconsListenerApi.ScanMode.BALANCED,
          askEnableBt = true,
          oneShot     = false,
        ),
      )
    }
    override def settings(isGranted: Boolean): List[SettingSet] = {
      val isGrantedJs = JsBoolean( isGranted )
      var btConfKeys = ConfConst.ScSettings.bluetoothKeys
        .map( SettingSet(_, isGrantedJs, save = true, runSideEffect = false) )

      // Android: bluetooth permission also activates geolocation.
      if (isGranted && (platformRO.value.osFamily contains[MOsFamily] MOsFamilies.Android))
        btConfKeys ::= SettingSet( ConfConst.ScSettings.LOCATION_ENABLED, isGrantedJs, save = true, runSideEffect = true )

      btConfKeys
    }
  }

  /** Notifications specification. */
  final class NotificationSpec extends IPermissionSpec {
    lazy val platform = platformRO.value
    // Notifications
    override def phase = MWzPhases.NotificationPerm
    override def isSupported(): Boolean = {
      (platform.isCordova && CordovaNotificationlLocalUtil.isCnlApiAvailable()) ||
      // Т.к. уведомления только по Bluetooth-маячкам, то нотификейшены не требуются в prod-режиме браузера.
      (platform.isBrowser && Html5NotificationUtil.isApiAvailable() && WzFirstDiaAh.NOTIFICATION_IN_BROWSER)
    }
    override def readPermissionState(): Future[IPermissionState] = {
      if (platform.isCordova) {
        CordovaNotificationlLocalUtil.hasPermissionState()
      } else {
        (if (h5PermApiAvail) {
          Try {
            Html5PermissionApi.getPermissionState( PermissionName.notifications )
          }
        } else {
          Failure( new UnsupportedOperationException )
        })
          .recover { case _ =>
            Html5NotificationUtil.getPermissionState()
          }
          .getOrElse( Future.failed(new UnsupportedOperationException) )
      }
    }
    override def requestPermissionFx: Effect =
      NotificationPermAsk( isVisible = true ).toEffectPure
    override def onGrantedByDefault = None
    override def settings(isGranted: Boolean) = {
      // runSideEffect = true causes NotifyStartStop(true), that not called previously.
      SettingSet( ConfConst.ScSettings.NOTIFICATIONS_ENABLED, JsBoolean(isGranted), save = true, runSideEffect = true ) :: Nil
    }
  }

  private def _allPhaseSpecs =
    new GeoLocationSpec #::
    new BlueToothSpec #::
    new NotificationSpec #::
    // End of permissions specs.
    LazyList.empty[IPermissionSpec]

  private def _findPhaseSpec(phase: MWzPhase) =
    _allPhaseSpecs.find(_.phase ==* phase)

  /** Start with asking these permissions? */
  private def _platformStartPhases(): Seq[MWzPhase] = {
    val acc0: List[MWzPhase] =
      MWzPhases.NotificationPerm ::
      Nil

    val plat = platformRO.value
    val phases = plat.osFamily.fold {
      // Browser: don't ask bluetooth on startup, no WebBluetooth support.
      acc0
    } {
      case MOsFamilies.Apple_iOS =>
        // On iOS: Ask for bluetooth access on start, because there are no case (no button) to ask it.
        MWzPhases.BlueToothPerm ::
        acc0
      case MOsFamilies.Android =>
        // On android, do not ask bluetooth permission. Enable it with location button, because bluetooth scanning needs FINE LOCATION permission.
        acc0
    }

    phases
  }

}


/** Внутренняя модель для спецификации одного пермишшена. */
sealed trait IPermissionSpec {
  /** id фазы. */
  def phase: MWzPhase
  /** Есть ли поддержка на уровне платформы? Синхронная проверка доступности соответствующего API. */
  def isSupported(): Boolean
  /** Чтение текущего состояния пермишена. Запуск асихнронного получения данных по пермишшену. */
  def readPermissionState(): Future[IPermissionState]
  /** Запрос у юзера права доступа. */
  def requestPermissionFx: Effect
  /** Если пермишшен уже выдан без запроса, то можно запустить дополнительно эффект: */
  def onGrantedByDefault: Option[Effect] =
    Some( requestPermissionFx )
  /** If defined, the Pot can be used for detection of permission state changes. */
  def listenChangesOfPot: Option[ModelR[MScRoot, Pot[_]]] = None
  /** Update saved settings, when permission grants or denies. */
  def settings(isGranted: Boolean): List[SettingSet]
}


object WzFirstDiaAh extends Log {

  // developmentMode - Т.к. уведомления только по Bluetooth-маячкам, то нотификейшены не требуются в prod-режиме браузера.
  /** Запрашивать доступ на нотификацию в браузере. */
  final def NOTIFICATION_IN_BROWSER = scalajs.LinkingInfo.developmentMode

  /** Если юзер вместо разрешения нажал "позже", то надо ли отображать info-окошко или перейти сразу на следующий шаг. */
  final def CRY_ABOUT_REFUSE_LATER = false

  /** Быстро выдать ответ: надо ли передавать управление запуском в контроллер визарда?
    * Тут только поверхностные проверки без углубления.
    * @return Фьючерс с мнением по поводу необходимости запуска цепочки мастера начальной настройки.
    */
  private def _isNeedWizardFlow(): Boolean = {
    val tryR = Try {
      // Если уже был запуск, то снова не надо.
      val frStoredOpt = MFirstRunStored.get()
      //LOG.log(s"stored=$frStoredOpt current=${MFirstRunStored.Versions.CURRENT} dev?${Sc3ConfUtil.isDevMode}")
      frStoredOpt.fold(true) { stored =>
        stored.version < MFirstRunStored.Versions.CURRENT
      } ||
        // Но надо, если dev-режим. И *после* запроса к БД, чтобы отладить сам запрос.
        Sc3ConfUtil.isDevMode
    }

    for (ex <- tryR.failed)
      logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, ex )

    tryR getOrElse true
  }


  lazy val isNeedWizardFlowVal = _isNeedWizardFlow()

}
