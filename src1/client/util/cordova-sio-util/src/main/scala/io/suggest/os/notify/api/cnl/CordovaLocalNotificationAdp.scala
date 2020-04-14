package io.suggest.os.notify.api.cnl

import cordova.plugins.notification.local.{CnlAction, CnlEventData, CnlMessage, CnlToast}
import diode.data.Pot
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.MsgCodes
import io.suggest.msg.ErrorMsgs
import io.suggest.os.notify.{CloseNotify, MOsToast, MOsToastActionEvent, NotifyPermission, NotifyStartStop, ShowNotify}
import io.suggest.primo.Keep
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import japgolly.univeq._

import scala.collection.immutable.HashMap
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.2020 10:18
  * Description: Адаптер для управления нотификациями через cordova API.
  */
final class CordovaLocalNotificationAdp[M](
                                            dispatcher    : Dispatcher,
                                            modelRW       : ModelRW[M, MCnlNotifierS],
                                          )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  import CordovaLocalNotificationlUtil.CNL

  /** Эффект обновления списка листенеров для нотификаций. */
  private def _updateCnlListenersFx(v0: MCnlNotifierS,
                                    toastsById2: HashMap[Int, MOsToast]): Option[Effect] = {
    // Определить, есть ли изменения в конфигурации listener'ов
    val needEventTypes = toastsById2
      .valuesIterator
      .flatMap { osToast =>
        osToast.onEvent.keysIterator #::
        osToast.actions.iterator.map(_.id) #::
        LazyList.empty
      }
      .flatten
      .toSet

    val listeningEventTypes = v0.listeners.keySet

    val notNeededEventTypes = listeningEventTypes -- needEventTypes
    val missingEventTypes = needEventTypes -- listeningEventTypes

    Option.when(notNeededEventTypes.nonEmpty || missingEventTypes.nonEmpty) {
      // Что-то в конфигурации listener'ов изменилось. Надо запустить эффект обновления листенеров.
      Effect.action {
        // Отписываемся от уже ненужных событий:
        val remove = (for {
          eventType <- notNeededEventTypes.iterator
          fn <- v0.listeners.get(eventType)
          _ <- {
            val offTry = Try( CNL.un( eventType, fn ) )
            for (ex <- offTry.failed)
              LOG.error( ErrorMsgs.NATIVE_API_ERROR, ex, (MsgCodes.`Off`, eventType) )
            offTry.toOption
          }
        } yield {
          eventType
        })
          .toSet

        // Подписываемся на новые необходимые листенеры:
        val add = (for {
          eventType <- missingEventTypes.iterator
          callback: js.Function = eventType match {
            case CnlEvents.CLEAR_ALL | CnlEvents.CANCEL_ALL =>
              {(evtData: CnlEventData) =>
                dispatcher.dispatch( HandleCnlEvent( evtData ) )
              }
            case _ =>
              {(toast: CnlToast, evtData: CnlEventData) =>
                dispatcher.dispatch( HandleCnlEvent( evtData, Some(toast) ) )
              }
          }
          _ <- {
            val onTry = Try( CNL.on( eventType, callback ) )
            for (ex <- onTry.failed)
              LOG.error( ErrorMsgs.NATIVE_API_ERROR, ex, msg = (MsgCodes.`On`, eventType) )
            onTry.toOption
          }
        } yield {
          eventType -> callback
        })
          .to( HashMap )

        UpdateCnlListeners( add, remove )
      }
    }
  }



  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Инициализация адаптера нотификаций и его состояния.
    case m: NotifyStartStop =>
      def reDoFx: Effect =
        Effect.action( NotifyStartStop.maxTries.modify(_ - 1)(m) )

      val v0 = value

      if (m.maxTries <= 0) {
        LOG.warn( ErrorMsgs.ENDLESS_LOOP_MAYBE, msg = m )
        noChange

      } else if ( m.isStart && CordovaLocalNotificationlUtil.isCnlApiAvailable()) {
        // Сразу узнать текущее состояние разрешений доступа:
        val fx = NotifyPermission(isRequest = false).toEffectPure
        effectOnly( fx )

      } else if (v0.listeners.nonEmpty) {
        val cancelFx = Effect.action {
          val listenersCancelled = (for {
            (eventType, jsFun) <- v0.listeners.iterator
            if {
              val tryUn = Try( CNL.un( eventType, jsFun ) )
              for (ex <- tryUn.failed)
                LOG.error( ErrorMsgs.NATIVE_API_ERROR, ex, (m, eventType) )
              tryUn.isSuccess
            }
          } yield {
            eventType
          })
            .toSet
          UpdateCnlListeners( HashMap.empty, removeListeners = listenersCancelled )
        }
        // Повторно запустить этот экшен, чтобы перейти на следующий шаг:
        effectOnly( cancelFx >> reDoFx )

      } else if (v0.toastsById.nonEmpty) {
        // Скрыть все уведомления:
        val clearToastsFx = Effect {
          for (_ <- CNL.clearAllF()) yield
            SetCnlToasts( Nil, append = false )
        }
        // Повторно запустить этот экшен, чтобы перейти на следующий шаг:
        val fxs = clearToastsFx >> reDoFx
        effectOnly( fxs )

      } else if (v0.permission.nonEmpty) {
        // Окончательное отключение: сброс данных по пермишшену.
        val v2 = MCnlNotifierS.permission.set( Pot.empty )(v0)
        updated( v2 )

      } else {
        noChange
      }


    // Отображение сообщений на экране.
    case m: ShowNotify =>
      val v0 = value
      // Получить последний использованный int-идентификатор нотификации.
      var counter1 = v0.lastId

      val cnlToasts9 = for {
        osToast <- m.toasts
      } yield {
        // Найти/сгенерить новый целочисленный id для текущей нотификации.
        // Если uid нотификации совпадает с существующей нотификацией, то нужно чтобы int id тоже совпадал:
        val _toastIntId = v0.toastUids.getOrElse(
          osToast.uid,
          {
            counter1 += 1
            counter1
          }
        )

        val _textsUndef = JsOptionUtil.maybeDefined[String | js.Array[CnlMessage]]( osToast.text.nonEmpty ) {
          osToast.text match {
            case Seq( justText ) if justText.person.isEmpty =>
              println( justText.text )
              justText.text
            case texts if texts.nonEmpty =>
              (for (osText <- texts.iterator) yield {
                new CnlMessage {
                  override val message = osText.text
                  override val person = osText.person.orUndefined
                }: CnlMessage
              })
                .toJSArray
          }
        }

        val _actionsUndef = JsOptionUtil.maybeDefined( osToast.actions.nonEmpty ) {
          (for (osNotAction <- osToast.actions) yield {
            new CnlAction {
              override val id = osNotAction.id
              override val title = osNotAction.title
              override val `type` = osNotAction.inputType.orUndefined
              override val emptyText = osNotAction.emptyText.orUndefined
              override val icon = osNotAction.iconUrl.orUndefined
            }: String | CnlAction
          })
            .toJSArray
        }

        val attachmentsUndef = osToast
          .imageUrl
          .map( js.Array(_) )
          .orUndefined

        val cnlToast = new CnlToast {
          override val id = _toastIntId
          override val title = osToast.title
          override val text = _textsUndef
          override val attachments = attachmentsUndef
          override val smallIcon = osToast.smallIconUrl.orUndefined
          override val icon = osToast.iconUrl.orUndefined
          override val data = osToast.data.orUndefined
          override val vibrate = osToast.vibrate.orUndefined
          override val silent = osToast.silent.orUndefined
          override val actions = _actionsUndef
          override val badge = osToast.appBadgeCounter.orUndefined
        }

        CnlToastInfo(_toastIntId, osToast, cnlToast)
      }

      // Отрендерить готовые сообщения:
      val cnlFx = Effect {
        // Следует рендерить поштучно или оптом? Если оптом и ошибка, то часть отобразится на экране, но
        // этот контроллер не будет знать об этом. Сейчас игнорим эту проблему.
        for {
          _ <- CNL.scheduleF(
            cnlToasts9
              .iterator
              .map(_.cnlToast)
              .toJSArray
          )
        } yield {
          // Сообщить контроллеру, что сообщения отображены.
          SetCnlToasts( cnlToasts9, append = true )
        }
      }

      var fxs: List[Effect] = cnlFx :: Nil

      // Обновить состояние адаптера:
      val v2 = (MCnlNotifierS.lastId set counter1)(v0)

      for (upListenFx <- _updateCnlListenersFx(v0, v2.toastsById))
        fxs ::= upListenFx

      ah.updatedSilentMaybeEffect( v2, fxs.mergeEffects )


    // Пришло какое-то событие из cordova-plugin:
    case m: HandleCnlEvent =>
      println(s"CNL event: $m")
      val v0 = value

      val eventType = m.data.event

      lazy val actionEvent = MOsToastActionEvent(
        text = m.data.text.toOption,
      )

      val toastOpt = m.data
        .notification
        .toOption
        .flatMap( v0.toastsById.get )

      // Нужно найти в карте нотификаций адресата для получения сообщения.
      var fxs: LazyList[Effect] = (for {
        toast <- toastOpt.iterator
        diodeAction <- {
          val events = (for {
            action <- toast.actions
            if action.id ==* eventType
            mkActionF <- action.onAction
          } yield {
            mkActionF( actionEvent )
          })
            .to( LazyList )

          toast.onEvent
            .get( eventType )
            .fold( events )( _ #:: events )
        }
      } yield {
        diodeAction.toEffectPure
      })
        .to( LazyList )

      // Если событие сокрытия нотификаций, то обновить состояние.
      val v2Opt: Option[MCnlNotifierS] = eventType match {
        // Убрать одно уведомление из состояния
        case CnlEvents.CANCEL | CnlEvents.CLEAR =>
          for {
            toastIntId <- m.data.notification.toOption
            toast <- toastOpt
            // Удалить указанный тост из состояния.
            v2 = (
              MCnlNotifierS.toastUids.modify(_ - toast.uid) andThen
              MCnlNotifierS.toastsById.modify(_ - toastIntId)
            )(v0)
          } yield {
            for (upListenFx <- _updateCnlListenersFx(v0, v2.toastsById))
              fxs #::= upListenFx

            v2
          }

        // Сокрытие всех уведомлений
        case CnlEvents.CANCEL_ALL | CnlEvents.CLEAR_ALL =>
          val v2 = (
            MCnlNotifierS.toastUids.set( HashMap.empty ) andThen
            MCnlNotifierS.toastsById.set( HashMap.empty )
          )(v0)

          for (upListenFx <- _updateCnlListenersFx(v0, v2.toastsById))
            fxs #::= upListenFx

          Some( v2 )

        case _ =>
          None
      }

      ah.optionalResult( v2Opt, fxs.mergeEffects, silent = true )


    // Скрыть перечисленные нотификации.
    case m: CloseNotify =>
      val v0 = value

      val toastIntIds = m.toastIds
        .iterator
        .flatMap( v0.toastUids.get )
        .toSet
        .toSeq

      if (toastIntIds.nonEmpty) {
        val fx = Effect {
          for (_ <- CNL.cancelF( toastIntIds: _* ) ) yield
            RmCnlToasts( m.toastIds, toastIntIds )
        }
        effectOnly( fx )
      } else {
        noChange
      }


    // Внутреннее стирание нотификаций из состояния.
    case m: RmCnlToasts =>
      val v0 = value
      val v2 = (
        MCnlNotifierS.toastsById.modify(_ -- m.intIds) andThen
        MCnlNotifierS.toastUids.modify(_ -- m.toastIds)
      )(v0)

      updatedSilent( v2 )


    // Сигнал, что нотификации отправлены в очередь вывода на экран.
    case m: SetCnlToasts =>
      val v0 = value

      // Залить обновлённые листенеры в состояние.
      val v2 = (
        // Обновить карту данных по нотификейшенам:
        MCnlNotifierS.toastsById.modify { toastsById0 =>
          val newToastsMap = m.toasts
            .iterator
            .map { cnlToastInfo =>
              cnlToastInfo.intId -> cnlToastInfo.osToast
            }
            .to( HashMap )

          if (m.append)
            toastsById0
              .merged( newToastsMap )( Keep.right )
          else
            newToastsMap
        } andThen
        // Обновить карту int-id'шников:
        MCnlNotifierS.toastUids.modify { toastsUids0 =>
          val newToastsToIdsMap = m.toasts
            .iterator
            .map { cnlToastInfo =>
              cnlToastInfo.osToast.uid -> cnlToastInfo.intId
            }
            .to( HashMap )

          if (m.append)
            toastsUids0
              .merged( newToastsToIdsMap )( Keep.right )
          else
            newToastsToIdsMap
        }
      )(v0)

      updatedSilent( v2 )


    // Обновление списка listener'ов.
    case m: UpdateCnlListeners =>
      val v0 = value
      val v2 = MCnlNotifierS.listeners.modify { listeners0 =>
        listeners0
          .removedAll( m.removeListeners )
          .merged( m.addListeners )( Keep.right )
      }(v0)
      updatedSilent( v2 )


    // Запрос текущего состояния разрешения на вывод уведомлений.
    case m: NotifyPermission =>
      println(m)

      // Залить pending в состояние, если там ещё не pending.
      val v0 = value
      val v2Opt = if (v0.permission.isPending) {
        None
      } else {
        Some( MCnlNotifierS.permission.modify(_.pending())(v0) )
      }

      // Расшаренное между эффектами значение проверки прав доступа:
      lazy val isAllowedOptFut = for {
        isAllowed <- if (m.isRequest) {
          // Явный запрос прав у юзера.
          CNL.requestPermissionF()
        } else {
          // Скрытое чтение текущег состояния прав доступа.
          CNL.hasPermissionF()
        }
      } yield {
        // Обернуть результат в Option[].
        // Если скрытая has-проверка, то false означает, только отсутствие разрешение, но не означает запрета.
        if (m.isRequest) OptionUtil.SomeBool( isAllowed )
        else OptionUtil.SomeBool.orNone( isAllowed )
      }

      // Надо поместить состояние контроллера результат запроса, если оно изменилось.
      var fx: Effect = Effect {
        for (isAllowedOpt <- isAllowedOptFut) yield {
          (for {
            // Нет смысла сохранять None в состояние.
            isAllowed <- isAllowedOpt
            // Notify-система должна быть активна.
            if !(v0.permission contains[Boolean] isAllowed)
          } yield {
            CnlSavePermission( isAllowedOpt )
          })
            .getOrElse( DoNothing )
        }
      }

      // Сначала - проброс вызова в нижележащее API, и возврат экшена с ответом без участия данного контроллера.
      for (onComplete <- m.onComplete) {
        val replySenderFx = Effect {
          for (isAllowedOpt <- isAllowedOptFut) yield
            onComplete( isAllowedOpt )
        }
        // fx0: Защита от бесконеч.цикла в будущем на случай, если >> станет на вход принимать функцию вместо эффекта.
        val fx0 = fx
        fx = replySenderFx >> fx0
      }

      ah.optionalResult( v2Opt, Some(fx) )


    // Сохранение значения пермишшена в состояние.
    case m: CnlSavePermission =>
      val v0 = value

      if (m.isAllowedOpt.isEmpty && !v0.permission.isPending) {
        noChange
      } else {
        val v2 = MCnlNotifierS.permission.modify { permPot0 =>
          m.isAllowedOpt
            .fold( permPot0.unPending )( permPot0.ready )
        }(v0)

        updated(v2)
      }

  }

}
