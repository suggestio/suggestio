package io.suggest.sc.c.dia

import cordova.Cordova
import diode._
import diode.Implicits._
import diode.data.{Pot, Ready}
import io.suggest.radio.beacon.{BtOnOff, IBeaconsListenerApi, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.MPlatformS
import io.suggest.geo.GeoLocUtilJs
import io.suggest.msg.ErrorMsgs
import io.suggest.os.notify.NotificationPermAsk
import io.suggest.os.notify.api.cnl.CordovaNotificationlLocalUtil
import io.suggest.os.notify.api.html5.Html5NotificationUtil
import io.suggest.perm.{CordovaDiagonsticPermissionUtil, Html5PermissionApi, IPermissionState}
import io.suggest.sc.m.{GeoLocOnOff, GeoLocTimerStart, MScRoot, ResetUrlRoute}
import io.suggest.sc.m.dia.first._
import io.suggest.log.Log
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.inx.MScSwitchCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.concurrent.{Future, TimeoutException}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import io.suggest.sc.u.Sc3ConfUtil
import org.scalajs.dom.experimental.permissions.PermissionName

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
class WzFirstDiaAh[M](
                       platformRO       : ModelRO[MPlatformS],
                       hasBleRO         : ModelRO[Boolean],
                       modelRW          : ModelRW[M, MWzFirstOuterS],
                       dispatcher       : Circuit[MScRoot],
                     )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Внутренняя модель для спецификации одного пермишшена. */
  private trait IPermissionSpec {
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
  }
  implicit private class PermSpecOpsExt( private val pss: IterableOnce[IPermissionSpec] ) {
    /** Найти утиль для пермишшена указанной wz-фазы. */
    def findPhase(phase: MWzPhase): Option[IPermissionSpec] =
      pss
        .iterator
        .find(_.phase ==* phase)
  }

  // Быстрое выставление фрейма.
  private def _setFrame(frame: MWzFrame, v0: MWzFirstOuterS, view0: MWzFirstS) = {
    val view2 = (MWzFirstS.frame set frame)(view0)
    (MWzFirstOuterS.view set Ready( view2 ).pending())(v0)
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
              _permPhasesSpecs()
                .findPhase( view0.phase )
                .map( _.requestPermissionFx )
                .fold {
                  // Нет фонового запроса доступа - нечего ожидать. Хотя этой ситуации быть не должно.
                  _wzGoToNextPhase( v0 )

                } { requestPermissionFx =>
                  // Эффект разблокировки диалога, чтобы крутилка ожидания не вертелась бесконечно.
                  val inProgressTimeoutFx = Effect {
                    // Если доступ поддерживает подписку на изменение статуса, то подписаться + frame=InProgress с долгим таймаутом.
                    // Если нет подписки, то InProgress + крутилку ожидания с коротким таймаутом в несколько секунд.
                    // Отказ юзера можно будет перехватить позже.
                    val inProgressTimeoutSec = (for {
                      permStatePot  <- v0.perms.get( view0.phase )
                      permState     <- permStatePot.toOption

                      if permState.hasOnChangeApi && {
                        // API доступно, но это не значит, что оно работает. Подписаться:
                        // TODO Надо бы перенести подписку прямо в WzPhasePermRes. Возможна ситуация, что диалог-мастер и фактический запрос геолокации отображаются одновременно.
                        val tryRes = Try(
                          permState.onChange { pss: IPermissionState =>
                            val action = WzPhasePermRes( view0.phase, Success(pss) )
                            dispatcher.dispatch( action )
                          }
                        )
                        for (ex <- tryRes.failed)
                          logger.warn( ErrorMsgs.PERMISSION_API_FAILED, ex, (permState, m) )
                        tryRes.isSuccess
                      }
                    } yield {
                      // Подписка удалась: надо InProgress с длинным таймаутом.
                      10
                    })
                      // Нет возможности подписаться на события. Надо InProgress закрывать по таймауту.
                      .getOrElse( 3 )

                    _permissionTimeout( view0.phase, inProgressTimeoutSec.seconds.toMillis.toInt )
                      .fut
                  }

                  // Переключить view в состояние ожидания.
                  val v2 = _setFrame( MWzFrames.InProgress, v0, view0 )

                  val fxs = requestPermissionFx + inProgressTimeoutFx
                  updated( v2, fxs )
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
      val v1: MWzFirstOuterS = MWzFirstOuterS.perms.modify { perms0 =>
        val permState2 = perms0
          .getOrElse( m.phase, Pot.empty )
          .withTry( m.res )
        perms0.updated( m.phase, permState2 )
      }(v0)

      // Additional effects for all branches, if any:
      val fxOpt = for {
        reason <- m.reason
        onCompleteFx <- reason.onComplete
        if !v1.perms
          .valuesIterator
          .exists(_.isPending)
      } yield {
        // No more pending permissions. Do onComplete effect, if any.
        onCompleteFx
      }

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
                val v2 = MWzFirstOuterS.view.set( Ready(
                  MWzFirstS.frame.set( MWzFrames.Info )(view0)
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


    // Start for dumping current permissions states into wizard state.
    case m: WzReadPermissions =>
      val v0 = value

      // Запускаемся.
      var fxsAcc: List[Effect] = Nil
      var permPotsAcc = List.empty[(MWzPhase, Pot[IPermissionState])]

      val someReason = Some(m)
      // Пройтись по списку фаз, активировав проверки прав:
      for {
        phaseSpec <- _permPhasesSpecs()
        if phaseSpec.isSupported()
        permPot0 = v0.perms.getOrElse( phaseSpec.phase, Pot.empty[IPermissionState] )
        // Prevent double-check until timeout: underlying native API may suffer from too simulateos calls.
        if !permPot0.isPending
      } {
        val timeoutP = _permissionTimeout( phaseSpec.phase, 3000 )
        val phasePermFx: Effect =
          _readPermissionFx( phaseSpec, reason = someReason ) +
          Effect( timeoutP.fut )
        fxsAcc ::= phasePermFx

        // Update current permission Pot[] state to pending:
        val permPot2 = permPot0
          .pending( timeoutP.timerId )
        permPotsAcc ::= (phaseSpec.phase -> permPot2)
      }

      // Start single timer with many actions for all pending tasks:
      (for {
        (phase, _) <- permPotsAcc.iterator
      } yield {
        Effect.action( WzPhasePermRes( phase, Failure(new TimeoutException), someReason ) )
      })
        .mergeEffects
        .foreach { fx =>
          fxsAcc ::= fx.after( 3.seconds )
        }

      val v2 = MWzFirstOuterS.perms.modify(_ ++ permPotsAcc)(v0)
      ah.updatedSilentMaybeEffect( v2, fxsAcc.mergeEffects )


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      val v0 = value
      if (
        m.showHide &&
        v0.view.isEmpty &&
        WzFirstDiaAh.isNeedWizardFlowVal
      ) {
        // Запускаемся.
        val fx: Effect =
          WzReadPermissions().toEffectPure >>
          // И надо бы выставить в URL отметку, что теперь открыт диалог:
          ResetUrlRoute(force = true).toEffectPure

        // Инициализировать состояние first-диалога.
        val first2 = (
          MWzFirstOuterS.view set Ready(MWzFirstS(
            phase = MWzPhases.Starting,
            frame = MWzFrames.InProgress,
          )).pending()
        )(v0)

        updated( first2, fx )

      } else if (!m.showHide && v0.view.nonEmpty) {
        // Closing opened first-run snack-dialog:
        val v2 = MWzFirstOuterS.view.modify(_.unavailable())(v0)

        // Ensure geolocation timer on first run after permissions are processed, etc:
        val fxOpt = Option.when( v2.perms.hasLocationAccess ) {
          Effect.action {
            GeoLocTimerStart(
              MScSwitchCtx(
                demandLocTest = false,
                indexQsArgs = MScIndexArgs(
                  geoIntoRcvr = true,
                )
              )
            )
          }
        }

        ah.updatedMaybeEffect( v2, fxOpt )

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

      // Найти и разобраться с pot с пермишшеном фазы:
      permPot <- v0.perms.get( nextPhase )

      // Основная логика упакована сюда:
      v9 <- permPot
        // По идее, тут или ready, или failed.
        .toOption
        .flatMap[MWzFirstOuterS] { perm =>
          if (perm.isPrompt) {
            // Надо задать вопрос юзеру, т.к. доступ ещё не запрашивался.
            val v2 = MWzFirstOuterS.view.set {
              Ready(
                view0.copy(
                  phase = nextPhase,
                  frame = MWzFrames.AskPerm
                )
              ).pending()
            }(v0)
            Some(v2)

          } else if (perm.isDenied) {
            // Запрещён доступ. Значит юзеру можно выразить сожаление в инфо-окне.
            val v2 = MWzFirstOuterS.view.set {
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
            if (perm.isGranted && nextPhase !=* view0.phase) {
              for {
                permSpec <- _permPhasesSpecs().iterator
                if permSpec.phase ==* nextPhase
                fx <- permSpec.onGrantedByDefault
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
        effectOnly( allFx )
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

  private def _readPermissionFx( phaseSpec: IPermissionSpec, reason: Option[WzReadPermissions] ): Effect = {
    Effect {
      phaseSpec
        .readPermissionState()
        .transform { tryPermState =>
          val action = WzPhasePermRes( phaseSpec.phase, tryPermState, reason )
          Success( action )
        }
    }
  }

  /** Start timer with permission phase error. */
  private def _permissionTimeout( phase: MWzPhase, timeoutMs: Double ) = {
    DomQuick
      .timeoutPromiseT( timeoutMs )(
        WzPhasePermRes( phase, Failure(new TimeoutException( ErrorMsgs.PERMISSION_REQUEST_TIMEOUT )) )
      )
  }

  /** Сборка спецификация по фазам, которые требуют проверки прав доступа. */
  private def _permPhasesSpecs(): LazyList[IPermissionSpec] = {
    lazy val platform = platformRO.value
    // Список спецификаций фаз с инструкциями, которые необходимо пройти для инициализации.
    lazy val h5PermApiAvail = Html5PermissionApi.isApiAvail()

    // Геолокация
    new IPermissionSpec {
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
        if (platform.isCordova) {
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
      //override def onGrantedByDefault = Some {
      //  requestPermissionFx + Effect.action {
      //    GeoLocTimerStart(MScSwitchCtx(MScIndexArgs(geoIntoRcvr = true)))
      //  }
      //}

    } #:: new IPermissionSpec {
      // Bluetooth
      override def phase = MWzPhases.BlueToothPerm
      override def isSupported() = hasBleRO()
      override def readPermissionState() =
        CordovaDiagonsticPermissionUtil.getBlueToothState()
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

    } #:: new IPermissionSpec {
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

    } #:: /*new IPermissionSpec {
      // NFC
      override def phase = MWzPhases.Nfc
      override def isSupported(): Boolean = {
        // TODO NFC Fully disabled (only dev mode) until it will be fully implemented.
        scalajs.LinkingInfo.developmentMode && nfcApi.exists(_.isApiAvailable())
      }
      override def readPermissionState(): Future[IPermissionState] =
        nfcApi.get.readPermissionState()
      override def requestPermissionFx =
        NfcScan( enabled = true ).toEffectPure
    } #:: */ LazyList.empty[IPermissionSpec]
  }

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
