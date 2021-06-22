package io.suggest.sc.c.dia

import cordova.Cordova
import diode._
import diode.data.{Pending, Pot}
import io.suggest.radio.beacon.{BtOnOff, IBeaconsListenerApi, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.{MPlatformS, MScreenInfo}
import io.suggest.geo.GeoLocUtilJs
import io.suggest.msg.ErrorMsgs
import io.suggest.os.notify.NotificationPermAsk
import io.suggest.os.notify.api.cnl.CordovaNotificationlLocalUtil
import io.suggest.os.notify.api.html5.Html5NotificationUtil
import io.suggest.perm.{CordovaDiagonsticPermissionUtil, Html5PermissionApi, IPermissionState}
import io.suggest.sc.m.{GeoLocOnOff, MScRoot, ResetUrlRoute}
import io.suggest.sc.m.dia.first._
import io.suggest.log.Log
import io.suggest.nfc.{INfcApi, NfcScan}
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

import scala.concurrent.{Future, TimeoutException}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scalaz.std.option._
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.v.dia.first.WzFirstCss
import monocle.Traversal
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
                       screenInfoRO     : ModelR[MScRoot, MScreenInfo],
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


  private def _subscribeCssRebuildFx: Effect = {
    Effect.action {
      val unSubscribeF = dispatcher.subscribe( screenInfoRO ) { _ =>
        dispatcher.dispatch( Wz1RebuildCss )
      }
      Wz1SetUnSubscribeF( unSubscribeF )
    }
  }

  private def _unSubsCribeCssRebuildFx( unSubsCribeF: () => Unit): Effect = {
    Effect.action {
      unSubsCribeF()
      DoNothing
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопкам в диалоге.
    case m: YesNoWz =>
      //println( m )
      val first0 = value
      first0.view.fold(noChange) { view0 =>

        // Быстрое выставление фрейма.
        def _setFrame(frame: MWzFrame) = {
          MWzFirstOuterS.view.set( Some(
            MWzFirstS.frame.set(frame)(view0)
          ))(first0)
        }

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
                  _wzGoToNextPhase( first0 )

                } { accessFx =>
                  // Эффект разблокировки диалога, чтобы крутилка ожидания не вертелась бесконечно.
                  val inProgressTimeoutFx = Effect {
                    // Если доступ поддерживает подписку на изменение статуса, то подписаться + frame=InProgress с долгим таймаутом.
                    // Если нет подписки, то InProgress + крутилку ожидания с коротким таймаутом в несколько секунд.
                    // Отказ юзера можно будет перехватить позже.
                    val inProgressTimeoutSec = (for {
                      permStatePot  <- first0.perms.get( view0.phase )
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

                    DomQuick
                      .timeoutPromiseT( inProgressTimeoutSec.seconds.toMillis.toInt )(
                        WzPhasePermRes(view0.phase, Failure(new TimeoutException( ErrorMsgs.PERMISSION_REQUEST_TIMEOUT )))
                      )
                      .fut
                  }

                  // Переключить view в состояние ожидания.
                  val v2 = _setFrame( MWzFrames.InProgress )

                  val fxs = accessFx + inProgressTimeoutFx
                  updated( v2, fxs )
                }

            // yes в info-окне означает retry, по идее.
            case MWzFrames.Info =>
              val v2 = _setFrame( MWzFrames.AskPerm )
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
              _wzGoToNextPhase( first0 )
            // Осознанный отказ в размещении - перейти в Info-фрейм текущей фазы
            case MWzFrames.AskPerm =>
              if (WzFirstDiaAh.CRY_ABOUT_REFUSE_LATER) {
                val v2 = _setFrame(MWzFrames.Info)
                updated(v2)
              } else {
                _wzGoToNextPhase( first0 )
              }
            // false во время InProgress - отмена ожидания. Надо назад перебросить, на Ask-шаг.
            case MWzFrames.InProgress =>
              val v2 = _setFrame( MWzFrames.AskPerm )
              // Отменить ожидание результата пермишена для текущей фазы:
              // TODO Унести управление onChange за пределы YesNo-сигнала.
              val fx = _wzUnWatchPermChangesFx( first0 )
              updated(v2, fx)
          }
        }

      }


    // Сигнал результата нативного диалога проверки прав или таймаута.
    case m: WzPhasePermRes =>
      val first00 = value
      first00.view.fold {
        // warn, т.к. это признак нарушения в процессе инициализации.
        logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, first00) )
        noChange

      } { view00 =>
        // Сохранить в состояние полученный снапшот с данными.
        val first0: MWzFirstOuterS = m.res match {
          // Нет смысла заливать таймаут в состояние.
          case Failure(_: NoSuchElementException) =>
            first00
          case _ =>
            val permState0 = first00.perms.getOrElse( m.phase, Pot.empty )
            val permState2 = permState0.withTry( m.res )
            MWzFirstOuterS.perms
              .modify( _.updated( m.phase, permState2 ) )(first00)
        }

        def _setFrame(frame: MWzFrame) = {
          MWzFirstOuterS.view.set( Some(
            MWzFirstS.frame.set( MWzFrames.Info )(view00)
          ))(first0)
        }

        // Обновлённый инстанс полного состояния. Нужен НЕ во всех ветвях:
        def v1 =
          if (first0 ===* first00) first00
          else first0

        // Надо понять, сейчас текущая фаза или какая-то другая уже. Всякое бывает.
        if (m.phase ==* view00.phase) {
          // Это текущая фаза.
          val isGrantedOpt =
            for (permState <- m.res.toOption)
            yield permState.isGranted

          view00.frame match {

            // Сейчас происходит ожидание ответа юзера в текущей фазе. Всё по плану. Но по плану ли ответ?
            case MWzFrames.InProgress =>
              if (isGrantedOpt contains[Boolean] false) {
                // Юзер не разрешил. Вывести Info с сожалением.
                val v2 = MWzFirstOuterS.view.set( Some(
                  MWzFirstS.frame.set( MWzFrames.Info )(view00)
                ))(first0)
                updated(v2)
              } else {
                // Положительный результат или отсутствие ответа. Просто перейти на следующую фазу:
                _wzGoToNextPhase( v1 )
              }

            // Ответ по timeout - игнорить за пределами InProgress
            case _ if m.res.isFailure =>
              noChange

            // Ответ от юзера - является ценным.
            case MWzFrames.Info =>
              if (isGrantedOpt contains[Boolean] true) {
                // Положительный ответ + Info => следующая фаза.
                _wzGoToNextPhase( v1 )
              } else {
                updatedSilent( v1 )
              }

            // Ответ юзера наступил во время вопроса текущей фазы.
            // Возможно, если разрешение было реально запрошено ещё какой-то подсистемой выдачи (за пределами этого диалога), косяк.
            case ph @ MWzFrames.AskPerm =>
              logger.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (ph, m) )
              if (isGrantedOpt contains[Boolean] true) {
                _wzGoToNextPhase( v1 )
              } else {
                val v2 = _setFrame( MWzFrames.Info )
                updated(v2)
              }

          }

        } else if (view00.phase ==* MWzPhases.Starting) {
          // Надо показать на экране текущий диалог в разном состоянии.
          val hasPending = first0.perms
            .exists(_._2.isPending)
          if (hasPending) {
            // Есть ещё pending в задачах. Просто убедиться, что диалог ожидания виден, оставаясь в Starting/InProgress.
            if (view00.visible) {
              // Диалог уже отображается, и ещё остались pending-задачи. Просто обнов
              updatedSilent( v1 )
            } else {
              // Ещё есть pending-задачи, но диалог скрыт. Показать диалог на экране, оставаясь в Starting-фазе:
              val v2 = MWzFirstOuterS.view.set(
                Some( MWzFirstS.visible.set( true )(view00) )
              )(first0)
              updated(v2)
            }

          } else {
            // Больше нет pending-задач. Переключиться на следующую фазу диалога.
            _wzGoToNextPhase( v1 )
          }

        } else {
          // Пока разрешения разруливались, фаза уже изменилась неизвестно куда. Не ясно, возможно ли такое на яву.
          // Пока просто молча пережёвываем.
          logger.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (m, view00.phase) )
          updatedSilent( v1 )
        }
      }


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      //println( m )
      val v0 = value
      if (
        m.showHide &&
        v0.view.isEmpty &&
        WzFirstDiaAh.isNeedWizardFlowVal
      ) {
        // Запускаемся.
        var fxsAcc: List[Effect] = Nil

        // Надо бы выставить в URL отметку, что теперь открыт диалог:
        fxsAcc ::= ResetUrlRoute(force = true).toEffectPure

        var permPotsAcc = List.empty[(MWzPhase, Pot[IPermissionState])]

        // Пройтись по списку фаз, активировав проверки прав:
        for {
          phaseSpec <- _permPhasesSpecs()
          if phaseSpec.isSupported()
        } {
          fxsAcc ::= Effect {
            phaseSpec
              .readPermissionState()
              .transform { tryPermState =>
                val action = WzPhasePermRes( phaseSpec.phase, tryPermState )
                Success( action )
              }
          }
          permPotsAcc ::= phaseSpec.phase -> Pending()
        }

        fxsAcc ::= _subscribeCssRebuildFx

        // Инициализировать состояние first-диалога.
        val first2 = MWzFirstOuterS(
          view = Some(MWzFirstS(
            visible = false,
            phase = MWzPhases.Starting,
            frame = MWzFrames.InProgress,
            css   = WzFirstCss( screenInfoRO.value.unsafeOffsets ),
            unSubscribe = Pot.empty.pending(),
          )),
          perms = permPotsAcc.toMap,
        )

        ah.updatedMaybeEffect( first2, fxsAcc.mergeEffects )

      } else if (!m.showHide && v0.view.isDefined) {
        // Закрываемся.
        val v2 = v0.copy(
          view  = None,
          perms = Map.empty,    // TODO По идее, perms уже не содержат полезных данных.
        )

        // Отписаться от событий изменений экрана.
        val fxOpt = for {
          v <- v0.view
          unSubsCribeF <- v.unSubscribe.toOption
        } yield {
          _unSubsCribeCssRebuildFx( unSubsCribeF )
        }

        ah.updatedMaybeEffect( v2, fxOpt )

      } else {
        logger.log( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      }


    case m: Wz1SetUnSubscribeF =>
      val v0 = value
      v0.view.fold {
        logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        val fx = _unSubsCribeCssRebuildFx( m.unSubscribeF )
        effectOnly( fx )

      } { _ =>
        val v2 = _wz1_outer_inner_TRAV
          .composeLens( MWzFirstS.unSubscribe )
          .modify( _.ready(m.unSubscribeF) )(v0)
        updatedSilent( v2 )
      }


    case Wz1RebuildCss =>
      val v0 = value

      (for {
        view <- v0.view
        args2 = screenInfoRO.value.unsafeOffsets
        if view.css.unsafeOffsets !=* args2
      } yield {
        val v2 = _wz1_outer_inner_TRAV
          .composeLens( MWzFirstS.css )
          .set( WzFirstCss( args2 ) )(v0)
        updatedSilent( v2 )
      })
        .getOrElse( noChange )

  }


  private def _wz1_outer_inner_TRAV = MWzFirstOuterS.view
    .composeTraversal( Traversal.fromTraverse[Option, MWzFirstS] )


  /** Перещёлкивание на следующую фазу диалога. */
  private def _wzGoToNextPhase(v0: MWzFirstOuterS): ActionResult[M] = {
    val view0 = v0.view.get
    var fxAcc = List.empty[Effect]

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
              Some(
                view0.copy(
                  phase = nextPhase,
                  visible = true,
                  frame = MWzFrames.AskPerm
                )
              )
            }(v0)
            Some(v2)

          } else if (perm.isDenied) {
            // Запрещён доступ. Значит юзеру можно выразить сожаление в инфо-окне.
            val v2 = MWzFirstOuterS.view.set {
              Some(
                view0.copy(
                  phase   = nextPhase,
                  visible = true,
                  frame   = MWzFrames.Info
                )
              )
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
          for (ex <- permPot.exceptionOption) yield {
            // Ошибка проверки фазы.
            // Для dev: вывести info-окошко с ошибкой.
            // Для prod: пока только логгирование.
            logger.log( ErrorMsgs.PERMISSION_API_FAILED, ex, nextPhase )

            // При ошибке - info-окно, чтобы там отрендерилась ошибка пермишена фазы?
            MWzFirstOuterS.view.set {
              OptionUtil.maybe( Sc3ConfUtil.isDevMode ) {
                view0.copy(
                  phase   = nextPhase,
                  visible = true,
                  frame   = MWzFrames.Info
                )
              }
            }(v0)
            // Можно пропускать фазу - наврядли end-юзер будет что-то дебажить.
          }
        }
        .orElse[MWzFirstOuterS] {
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
        val saveFx = Effect.action {
          Try {
            MFirstRunStored.save(
              MFirstRunStored(
                version = MFirstRunStored.Versions.CURRENT
              )
            )
          }
          // Надо вычистить все onChange-подписки на пермишшены.
          Try {
            for {
              permPot <- v0.perms.valuesIterator
              perm    <- permPot.iterator
            }
              perm.onChangeReset()
          }
          DoNothing
        }
        // Удаление из DOM
        val unRenderFx = InitFirstRunWz(false).toEffectPure

        // Надо скрыть диалог анимированно:
        val d2 = MWzFirstOuterS.view.set(
          Some( (MWzFirstS.visible set false)( view0 ) )
        )(v0)
        val allFx = (saveFx :: unRenderFx :: fxAcc)
          .mergeEffects
          .get
        updated( d2, allFx )
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


  /** Сборка спецификация по фазам, которые требуют проверки прав доступа. */
  private def _permPhasesSpecs(): LazyList[IPermissionSpec] = {
    val platform = platformRO.value
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
      override def requestPermissionFx: Effect =
        GeoLocOnOff( enabled = true, isHard = true ).toEffectPure

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
  def isNeedWizardFlow(): Boolean = {
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


  lazy val isNeedWizardFlowVal = isNeedWizardFlow()

}
