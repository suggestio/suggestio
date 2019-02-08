package io.suggest.sc.c.dia

import diode._
import diode.data.{Pending, Pot}
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MPlatformS
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.perm.{CordovaDiagonsticPermissionUtil, Html5PermissionApi, IPermissionState}
import io.suggest.sc.m.GeoLocOnOff
import io.suggest.sc.m.dia.first._
import io.suggest.sc.m.dia._
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

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
class FirstRunDialogAh[M](
                           platformRO       : ModelRO[MPlatformS],
                           modelRW          : ModelRW[M, Option[MWzFirstOuterS]],
                           dispatcher       : Dispatcher,
                         )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Внутренняя модель для спецификации пермишшена.
    *
    * @param supported Есть ли поддержка на уровне платформы?
    * @param phase id фазы.
    * @param askPermF Функция запуска асихнронного получения данных по пермишшену.
    */
  private case class PermissionSpec(
                                     supported          : Boolean,
                                     phase              : MWzPhase,
                                     askPermF           : () => Future[IPermissionState]
                                   )

  // Хелперы во избежание множественного инлайнинга одного и того же.
  /** Быстрый доступ к v0.view.frame */
  private def _viewFrameLens(frame: MWzFrame) = {
    MWzFirstOuterS.view
      .composeLens( MWzFirstS.frame )
      .set(frame)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопкам в диалоге.
    case m: YesNoWz =>
      //println( m )
      val v0 = value
      v0.fold(noChange) { first0 =>

        if (m.yesNo) {
          // Положительный ответ - запустить запрос разрешения.
          first0.view.frame match {
            case MWzFrames.AskPerm =>
              // Положительный ответ - запросить доступ в реальности.
              val accessFxOpt = _requestAccessFx( first0.view.phase )

              accessFxOpt.fold {
                // Нет фонового запроса доступа - нечего ожидать. Хотя этой ситуации быть не должно.
                _wzGoToNextPhase(first0)
              } { accessFx =>
                // Эффект разблокировки диалога, чтобы крутилка ожидания не вертелась бесконечно.
                val inProgressTimeoutFx = Effect {
                  // Если доступ поддерживает подписку на изменение статуса, то подписаться + frame=InProgress с долгим таймаутом.
                  // Если нет подписки, то InProgress + крутилку ожидания с коротким таймаутом в 1-2 секунды.
                  // Отказ юзера можно будет перехватить позже.
                  val inProgressTimeoutSec = first0.perms
                    .get( first0.view.phase )
                    .flatMap(_.toOption)
                    .filter(_.hasOnChangeApi)
                    .filter { permState =>
                      // API доступно, но это не значит, что оно работает. Подписаться:
                      // TODO Надо бы перенести подписку прямо в WzPhasePermRes. Возможна ситуация, что диалог-мастер и фактический запрос геолокации отображаются одновременно.
                      val tryRes = Try(
                        permState.onChange { pss: IPermissionState =>
                          dispatcher.dispatch(
                            WzPhasePermRes( first0.view.phase, Success(pss) )
                          )
                        }
                      )
                      for (ex <- tryRes.failed)
                        LOG.warn( ErrorMsgs.PERMISSION_API_FAILED, ex, (permState, m) )
                      tryRes.isSuccess
                    }
                    // Если, нет возможности подписаться на события, то надо InProgress закрывать по таймаут.
                    // Когда подписка удалась, надо InProgress с длинным таймаутом.
                    // TODO 5 увеличить до 10 секунд, когда в браузерах стабилизируется PermissionStatus.onchange
                    .fold [Int](2)(_ => 5)

                  DomQuick
                    .timeoutPromiseT( inProgressTimeoutSec.seconds.toMillis )(
                      WzPhasePermRes(first0.view.phase, Failure(new NoSuchElementException))
                    )
                    .fut
                }

                // Переключить view в состояние ожидания.
                val v2 = Some(
                  _viewFrameLens( MWzFrames.InProgress )(first0)
                )

                val fxs = accessFx + inProgressTimeoutFx
                updated( v2, fxs )
              }

            // yes в info-окне означает retry, по идее.
            case MWzFrames.Info =>
              val v2 = Some(
                _viewFrameLens( MWzFrames.AskPerm )(first0)
              )
              updated( v2 )

            // yes во время InProgress. Недопустимо, ожидание можно только отменить.
            case other @ MWzFrames.InProgress =>
              LOG.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (other, m) )
              noChange
          }

        } else {
          first0.view.frame match {
            // Закрытие окошка с инфой - переход на след.фазу
            case MWzFrames.Info =>
              _wzGoToNextPhase( first0 )
            // Осознанный отказ в размещении - перейти в Info-фрейм текущей фазы
            case MWzFrames.AskPerm =>
              val v2 = Some(
                _viewFrameLens( MWzFrames.Info )(first0)
              )
              updated(v2)
            // false во время InProgress - отмена ожидания. Надо назад перебросить, на Ask-шаг.
            case MWzFrames.InProgress =>
              val v2 = Some(
                _viewFrameLens( MWzFrames.AskPerm )(first0)
              )
              // Отменить ожидание результата пермишена для текущей фазы:
              // TODO Унести управление onChange за пределы YesNo-сигнала.
              val fx = _wzUnWatchPermChangesFx( first0 )
              updated(v2, fx)
          }
        }

      }


    // Сигнал результата нативного диалога проверки прав или таймаута.
    case m: WzPhasePermRes =>
      val v00 = value
      v00.fold(noChange) { first00 =>
        // Сохранить в состояние полученный снапшот с данными.
        val first0: MWzFirstOuterS = m.res match {
          // Нет смысла заливать таймаут в состояние.
          case Failure(_: NoSuchElementException) =>
            first00
          case _ =>
            val permState0 = first00.perms.getOrElse( m.phase, Pot.empty )
            val permState2 = m.res.fold( permState0.fail, permState0.ready )
            MWzFirstOuterS.perms
              .modify( _.updated( m.phase, permState2 ) )(first00)
        }

        // Обновлённый инстанс полного состояния. Нужен НЕ во всех ветвях:
        def v1 =
          if (first0 ===* first00) first00
          else first0

        // Надо понять, сейчас текущая фаза или какая-то другая уже. Всякое бывает.
        if (m.phase ==* first0.view.phase) {
          val isGrantedOpt = m.res.toOption.map(_.isGranted)

          // Это текущая фаза.
          first0.view.frame match {

            // Сейчас происходит ожидание ответа юзера в текущей фазе. Всё по плану. Но по плану ли ответ?
            case MWzFrames.InProgress =>
              if (isGrantedOpt contains false) {
                // Юзер не разрешил. Вывести Info с сожалением.
                val v2 = Some(
                  _viewFrameLens( MWzFrames.Info )(first0)
                )
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
              if (isGrantedOpt contains true) {
                // Положительный ответ + Info => следующая фаза.
                _wzGoToNextPhase( v1 )
              } else {
                updatedSilent( Some(v1) )
              }

            // Ответ юзера наступил во время вопроса текущей фазы.
            // Возможно, если разрешение было реально запрошено ещё какой-то подсистемой выдачи (за пределами этого диалога), косяк.
            case ph @ MWzFrames.AskPerm =>
              LOG.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (ph, m) )
              if (isGrantedOpt contains true) {
                _wzGoToNextPhase( v1 )
              } else {
                val v2 = Some(
                  _viewFrameLens( MWzFrames.Info )(first0)
                )
                updated(v2)
              }

          }

        } else if (first0.view.phase ==* MWzPhases.Starting) {
          // TODO Чтобы объеденить экшен с PermissionState, надо тут разрулить фазу Starting.
          // Надо показать на экране текущий диалог в разном состоянии.
          val hasPending = first0.perms.exists(_._2.isPending)
          if (hasPending) {
            // Есть ещё pending в задачах. Просто убедиться, что диалог ожидания виден, оставаясь в Starting/InProgress.
            if (first0.view.visible) {
              // Диалог уже отображается, и ещё остались pending-задачи. Просто обнов
              updatedSilent( Some(v1) )
            } else {
              // Ещё есть pending-задачи, но диалог скрыт. Показать диалог на экране, оставаясь в Starting-фазе:
              val v2 = Some(
                MWzFirstOuterS.view
                  .composeLens( MWzFirstS.visible )
                  .set(true)(first0)
              )
              updated(v2)
            }

          } else {
            // Больше нет pending-задач. Переключиться на следующую фазу диалога.
            _wzGoToNextPhase( v1 )
          }

        } else {
          // Пока разрешения разруливались, фаза уже изменилась неизвестно куда. Не ясно, возможно ли такое на яву.
          // Пока просто молча пережёвываем.
          LOG.warn( ErrorMsgs.PERMISSION_API_LOGIC_INVALID, msg = (m, first0.view.phase) )
          updatedSilent( Some(v1) )
        }
      }


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      //println( m )
      val v0 = value
      if (
        m.showHide &&
        v0.isEmpty && (
          // Если уже был запуск, то снова не надо.
          MFirstRunStored.get().fold(true) { stored =>
            stored.version < MFirstRunStored.Versions.CURRENT
          } ||
          // Но надо, если dev-режим. И *после* запроса к БД, чтобы отладить сам запрос.
          scalajs.LinkingInfo.developmentMode
        )
      ) {
        // Акк для эффектов:
        var fxsAcc = List.empty[Effect]
        var permPotsAcc = List.empty[(MWzPhase, Pot[IPermissionState])]

        // Пройтись по списку фаз, активировав проверки прав:
        for {
          phaseSpec <- _permPhasesSpecs()
          if phaseSpec.supported
        } {
          fxsAcc ::= Effect {
            phaseSpec
              .askPermF()
              .transform { tryPermState =>
                val action = WzPhasePermRes( phaseSpec.phase, tryPermState )
                Success( action )
              }
          }
          permPotsAcc ::= phaseSpec.phase -> Pending()
        }

        // Инициализировать состояние first-диалога.
        val first2 = MWzFirstOuterS(
          view = MWzFirstS(
            visible = false,
            phase = MWzPhases.Starting,
            frame = MWzFrames.InProgress,
          ),
          perms = permPotsAcc.toMap
        )
        val v2 = Some(first2)
        ah.updatedMaybeEffect( v2, fxsAcc.mergeEffects )

      } else if (!m.showHide && v0.isDefined) {
        updated( None )

      } else {
        LOG.log( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      }

  }


  /** Сборка эффекта реального запросить доступа в зависимости от фазы. */
  private def _requestAccessFx(phase: MWzPhase): Option[Effect] = {
    phase match {
      case MWzPhases.GeoLocPerm =>
        val fx = GeoLocOnOff( enabled = true, isHard = true )
          .toEffectPure
        Some(fx)
      case MWzPhases.BlueToothPerm =>
        val fx = BtOnOff( isEnabled = true )
          .toEffectPure
        Some(fx)
      case _ =>
        None
    }
  }


  /** Перещёлкивание на следующую фазу диалога. */
  private def _wzGoToNextPhase(v0: MWzFirstOuterS): ActionResult[M] = {
    val currPhase = v0.view.phase
    val allPhases = MWzPhases.values

    allPhases
      .iterator
      // Получить итератор фаз ПОСЛЕ текущей фазы:
      .dropWhile { nextPhase =>
        nextPhase !=* currPhase
      }
      .drop(1)
      // Найти и разобраться с pot с пермишшеном фазы:
      .flatMap { nextPhase =>
        for {
          permPot <- v0.perms.get( nextPhase )

          // Основная логика упакована сюда:
          v9 <- {
            // По идее, тут или ready, или failed.
            if (permPot.nonEmpty) {
              // Есть значение - проанализировать значение.
              val perm = permPot.get
              if (perm.isPrompt) {
                // Надо задать вопрос юзеру, т.к. доступ ещё не запрашивался.
                val v2 = MWzFirstOuterS.view.modify {
                  _.copy(
                    phase = nextPhase,
                    visible = true,
                    frame = MWzFrames.AskPerm
                  )
                }(v0)
                Some(v2)
              } else if (perm.isDenied) {
                // Запрещён доступ. Значит юзеру можно выразить сожаление в инфо-окне.
                val v2 = MWzFirstOuterS.view.modify {
                  _.copy(
                    phase   = nextPhase,
                    visible = true,
                    frame   = MWzFrames.Info
                  )
                }(v0)
                Some(v2)
              } else {
                // granted или что-то неведомое - пропуск фазы.
                None
              }

            } else if (permPot.isFailed) {
              // Ошибка проверки фазы.
              // Для dev: вывести info-окошко с ошибкой.
              // Для prod: пока только логгирование.
              val ex = permPot.exceptionOption.get
              LOG.log( ErrorMsgs.PERMISSION_API_FAILED, ex, nextPhase )

              // При ошибке - info-окно, чтобы там отрендерилась ошибка пермишена фазы?
              OptionUtil.maybe( scalajs.LinkingInfo.developmentMode ) {
                MWzFirstOuterS.view.modify {
                  _.copy(
                    phase   = nextPhase,
                    visible = true,
                    frame   = MWzFrames.Info
                  )
                }(v0)
              }
              // Можно пропускать фазу - наврядли end-юзер будет что-то дебажить.

            } else {
              // pending|empty - тут быть не должно, т.к. код вызывается после всех проверок.
              LOG.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = permPot )
              None
            }
          }
        } yield {
          val d2 = Some(v9)
          // Следующая фаза одобрена:
          updated(d2)
        }
      }
      .buffered
      .headOption
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
        // Удаление из DOM с задержкой.
        val unRenderFx = Effect {
          DomQuick
            .timeoutPromiseT( 1.seconds.toMillis ) {
              InitFirstRunWz(false)
            }
            .fut
        }
        // Надо скрыть диалог анимированно:
        val d2 = Some(
          MWzFirstOuterS.view
            .composeLens( MWzFirstS.visible )
            .set(false)(v0)
        )
        val allFx = saveFx + unRenderFx
        updated( d2, allFx )
      }
  }


  /** Сброс мониторинга изменений права доступа. */
  private def _wzUnWatchPermChangesFx(first0: MWzFirstOuterS): Effect = {
    Effect.action {
      for (permPot <- first0.perms.get( first0.view.phase ))
        for (perm <- permPot if perm.hasOnChangeApi)
          Try(perm.onChangeReset())
      DoNothing
    }
  }


  /** Сборка спецификация по фазам, которые требуют проверки прав доступа. */
  private def _permPhasesSpecs(platform: MPlatformS = platformRO.value): Seq[PermissionSpec] = {
    // Список спецификаций фаз с инструкциями, которые необходимо пройти для инициализации.

    // Геолокация
    PermissionSpec(
      phase     = MWzPhases.GeoLocPerm,
      supported = platform.hasGeoLoc,
      askPermF  =
        if (platform.isCordova) CordovaDiagonsticPermissionUtil.getGeoLocPerm
        else Html5PermissionApi.getGeoLocPerm
    ) #::
    // Bluetooth
    PermissionSpec(
      phase     = MWzPhases.BlueToothPerm,
      supported = platform.hasBle,
      askPermF  = CordovaDiagonsticPermissionUtil.getBlueToothState
    ) #::
    Stream.empty[PermissionSpec]
  }

}


object FirstRunDialogAh extends Log {

  /** Быстро выдать ответ: надо ли передавать управление запуском в контроллер визарда?
    * Тут только поверхностные проверки без углубления.
    * @return Фьючерс с мнением по поводу необходимости запуска цепочки мастера начальной настройки.
    */
  def isNeedWizardFlow(): Boolean = {
    val tryR = Try {
      // Если уже был запуск, то снова не надо.
      MFirstRunStored.get().fold(true) { stored =>
        stored.version < MFirstRunStored.Versions.CURRENT
      } ||
      // Но надо, если dev-режим. И *после* запроса к БД, чтобы отладить сам запрос.
      scalajs.LinkingInfo.developmentMode
    }

    for (ex <- tryR.failed)
      LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, ex )

    tryR.getOrElse(true)
  }

}
