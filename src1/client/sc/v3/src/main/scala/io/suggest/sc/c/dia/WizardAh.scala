package io.suggest.sc.c.dia

import diode._
import diode.data.{Pot, Ready}
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
import japgolly.univeq._

import scala.util.{Success, Try}
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
class WizardAh[M](
                   platformRO       : ModelRO[MPlatformS],
                   modelRW          : ModelRW[M, MScDialogs],
                   dispatcher       : Dispatcher,
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопкам в диалоге.
    case m: YesNoWz =>
      //println( m )
      val v0 = value
      v0.first.fold(noChange) { first0 =>

        if (m.yesNo) {
          // Положительный ответ - запустить запрос разрешения.
          first0.view.frame match {
            case MWzFrames.AskPerm =>
              // Положительный ответ - запросить доступ в реальности.
              val accessFxOpt = first0.view.phase match {
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

              accessFxOpt.fold {
                // Нет фонового запроса доступа - нечего ожидать. Хотя этой ситуации быть не должно.
                _wzGoToNextPhase(v0)
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
                            WzPhasePermRes( first0.view.phase, Some(pss) )
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
                      WzPhasePermRes(first0.view.phase, None)
                    )
                    .fut
                }

                // Переключить view в состояние ожидания.
                val v2 = v0.withFirst( Some(
                  first0.withView(
                    first0.view
                      .withFrame( MWzFrames.InProgress )
                  )
                ))

                val fxs = accessFx + inProgressTimeoutFx
                updated( v2, fxs )
              }

            // yes в info-окне означает retry, по идее.
            case MWzFrames.Info =>
              val v2 = v0.withFirst(Some(
                first0.withView(
                  first0.view.withFrame( MWzFrames.AskPerm )
                )
              ))
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
              _wzGoToNextPhase(v0)
            // Осознанный отказ в размещении - перейти в Info-фрейм текущей фазы
            case MWzFrames.AskPerm =>
              val v2 = v0.withFirst( Some(
                first0.withView(
                  first0.view
                    .withFrame( MWzFrames.Info )
                )
              ))
              updated(v2)
            // false во время InProgress - отмена ожидания. Надо назад перебросить, на Ask-шаг.
            case MWzFrames.InProgress =>
              val v2 = v0.withFirst( Some(
                first0.withView(
                  view = first0.view
                    .withFrame( MWzFrames.AskPerm )
                )
              ))
              // Отменить ожидание результата пермишена для текущей фазы:
              // TODO Унести управление onChange за пределы YesNo-сигнала.
              val fx = _wzUnWatchPermChangesFx( first0 )
              updated(v2, fx)
          }
        }

      }


    // Сигнал результата нативного диалога проверки прав или таймаута.
    case m: WzPhasePermRes =>
      //println( m )
      val v00 = value
      v00.first.fold(noChange) { first00 =>
        // Сохранить в состояние полученный снапшот с данными.
        val first0 = m.res.fold(first00) { pss =>
          first00.withPerms(
            first00.perms
              .updated( m.phase, Ready(pss) )
          )
        }

        // Обновлённый инстанс полного состояния. Нужен НЕ во всех ветвях:
        def v1 = v00.withFirst(Some(first0))

        // Надо понять, сейчас текущая фаза или какая-то другая уже. Всякое бывает.
        if (m.phase ==* first0.view.phase) {
          // Это текущая фаза.
          first0.view.frame match {
            case MWzFrames.InProgress =>
              // Сейчас происходит ожидание ответа юзера в текущей фазе. Всё по плану. Но по плану ли ответ?
              if (m.res contains false) {
                // Юзер не разрешил. Вывести Info с сожалением.
                val v2 = v00.withFirst( Some(
                  first0.withView(
                    first0.view
                      .withFrame( MWzFrames.Info )
                  )
                ))
                updated(v2)
              } else {
                // Положительный результат или отсутствие ответа. Просто перейти на следующую фазу:
                _wzGoToNextPhase( v1 )
              }

            // Ответ по timeout - игнорить за пределами InProgress
            case _ if m.res.isEmpty =>
              noChange

            // Ответ от юзера - является ценным.
            case MWzFrames.Info =>
              if (m.res contains true) {
                // Положительный ответ + Info => следующая фаза.
                _wzGoToNextPhase( v1 )
              } else if (m.res.nonEmpty) {
                updatedSilent( v1 )
              } else {
                // Таймаут + Info => игнор, т.к. сожаление уже на экране.
                noChange
              }

            // Ответ юзера во время вопроса текущей фазы. Чудеса, но бывает...
            case MWzFrames.AskPerm =>
              if (m.res contains true) {
                _wzGoToNextPhase( v1 )
              } else {
                val v2 = v00.withFirst( Some(
                  first0.withView(
                    first0.view
                      .withFrame( MWzFrames.Info )
                  )
                ))
                updated(v2)
              }

          }

        } else {
          // Пока разрешения разруливались, фаза уже изменилась. Не ясно, возможно ли такое на яву.
          // Пока просто молча пережёвываем.
          noChange
        }
      }


    // Управление фоновой инициализацией:
    case m: InitFirstRunWz =>
      //println( m )
      val v0 = value
      if (
        m.isRendered &&
        v0.first.isEmpty && (
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

        val platform = platformRO.value

        // Если есть геолокация, то
        val hasGeoLoc = platform.hasGeoLoc
        val emptyPermPot = Pot.empty[IPermissionState]

        // Если поддерживается геолокация, то дополнить эффект и состояние:
        if (hasGeoLoc) {
          val phase = MWzPhases.GeoLocPerm
          fxsAcc ::= Effect {
            val fut0 = if (platform.isCordova) {
              CordovaDiagonsticPermissionUtil.getGeoLocPerm()
            } else {
              Html5PermissionApi.getGeoLocPerm()
            }
            fut0.transform { tryPermState =>
              val action = PermissionState( tryPermState, phase )
              Success( action )
            }
          }
          permPotsAcc ::= phase -> emptyPermPot.pending()
        }

        // Если поддерживается bluetooth, то заэффектить и обновить состояние.
        val hasBt = platform.hasBle
        if (hasBt) {
          val phase = MWzPhases.BlueToothPerm
          fxsAcc ::= Effect {
            CordovaDiagonsticPermissionUtil
              .getBlueToothState()
              .transform { tryPermState =>
                val action = PermissionState( tryPermState, phase )
                Success( action )
              }
          }
          permPotsAcc ::= phase -> emptyPermPot.pending()
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
        val v2 = v0.withFirst( Some(first2) )
        ah.updatedMaybeEffect( v2, fxsAcc.mergeEffects )

      } else if (!m.isRendered && v0.first.isDefined) {
        val v2 = v0.withFirst( None )
        updated(v2)

      } else {
        LOG.log( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      }


    // Пришёл результат считывания состояния какого-то пермишшена.
    case m: PermissionState =>
      val v0 = value
      v0.first.fold {
        LOG.log( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      } { first0 =>
        // Если диалог уже отображается, то запихнуть результат в pot, чтобы контроллер обработал их последующем шаге.
        first0
          .perms
          .get( m.phase )
          .fold {
            // Почему-то не ожидается данного ответа. По идее, такое не должно никогда происходить.
            LOG.warn( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = (m, first0.view) )
            noChange

          } { permPot0 =>
            // Залить результат чтения пермишшена в карту.
            val perms2 = first0.perms
              .updated( m.phase, m.tryPerm.fold(permPot0.fail, permPot0.ready) )

            // Надо показать на экране текущий диалог в разном состоянии.
            val hasPending = perms2.exists(_._2.isPending)
            if (hasPending) {
              // Есть ещё pending в задачах. Просто убедиться, что диалог ожидания виден, оставаясь в Starting/InProgress.
              val first2 = first0.copy(
                view =
                  if (first0.view.visible) first0.view
                  else first0.view.withVisible( true ),
                perms = perms2
              )
              val v2 = v0.withFirst( Some(first2) )
              updated(v2)

            } else {
              // Больше нет pending. Переключиться на следующую фазу диалога.
              val v2 = v0.withFirst( Some(
                first0.withPerms( perms2 )
              ))
              _wzGoToNextPhase( v2 )
            }
          }
      }

  }


  /** Перещёлкивание на следующую фазу диалога. */
  private def _wzGoToNextPhase(d0: MScDialogs): ActionResult[M] = {
    val v0 = d0.first.get
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
                val v2 = v0.withView(
                  v0.view.copy(
                    phase = nextPhase,
                    visible = true,
                    frame = MWzFrames.AskPerm
                  )
                )
                Some(v2)
              } else if (perm.isDenied) {
                // Запрещён доступ. Значит юзеру можно выразить сожаление в инфо-окне.
                val v2 = v0.withView(
                  v0.view.copy(
                    phase   = nextPhase,
                    visible = true,
                    frame   = MWzFrames.Info
                  )
                )
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
                v0.withView(
                  v0.view.copy(
                    phase   = nextPhase,
                    visible = true,
                    frame   = MWzFrames.Info
                  )
                )
              }
              // Можно пропускать фазу - наврядли end-юзер будет что-то дебажить.

            } else {
              // pending|empty - тут быть не должно, т.к. код вызывается после всех проверок.
              LOG.error( ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, msg = permPot )
              None
            }
          }
        } yield {
          val d2 = d0.withFirst( Some(v9) )
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
          for (permPot <- v0.perms.valuesIterator; perm <- permPot.iterator)
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
        val d2 = d0.withFirst( Some(
          v0.withView(
            v0.view
              .withVisible(false)
          )
        ))
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

}
