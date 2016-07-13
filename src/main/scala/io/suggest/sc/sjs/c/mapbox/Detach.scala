package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mmap.{EnsureMap, ScInxWillSwitch}
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoContent
import io.suggest.sjs.common.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.07.16 17:12
  * Description: Трейт Detached-состояний, когда готовая к работе извлечена из DOM,
  * чтобы повторно контейнер карта не пере-инициализировать.
  */
trait Detach extends Init {

  /** Трейт для сборки состояния, поддерживающего detach-контейнера карты при наступлении необходимости в этом. */
  trait HandleScInxSwitch extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case ScInxWillSwitch =>
        _handleScInxWillSwitch()
    }

    def _handleScInxWillSwitch(): Unit = {
      val sgContOpt = SGeoContent.find()
      val mapContOpt = sgContOpt.filter(_.nonEmpty)

      val nextState = _stateData.glmap.fold[FsmState] {
        // Карта ещё не инициализировалась.
        _noMapState

      } { glMap =>
        // Карта уже инициализировалась ранее, найти её контейнер в DOM.
        val sd0 = _stateData

        mapContOpt.fold [ FsmState] {
          // Почему-то контейнер карты пустой или не найден вообще. Хотя карта уже должна быть инициализирована.
          warn( WarnMsgs.MAP_ELEM_MISSING_BUT_EXPECTED )

          // Попытаться опустить имеющуюся карты, высвободив её ресурсы.
          try {
            glMap.remove()
          } catch {
            case ex: Throwable =>
              warn( WarnMsgs.MAP_SHUTDOWN_FAILED, ex )
          }

          // Переключиться на состояние ожидания сигнала о начальной инициализации.
          _stateData = sd0.copy(
            glmap = None
          )
          _noMapState

        } { _ =>
          // Есть контейнер карты с картой внутри. Забэкапить текущую карту, переключится на состояние ожидания смены index'а.
          _stateData = sd0.copy(
            detached = mapContOpt
          )
          _detachedState
        }
      }

      become(nextState)
    }

    /** Состояние отсоединенности контейнера карты от DOM. */
    def _detachedState: FsmState

    /** Состояние, когда нет инициализированной карты вообще. */
    def _noMapState: FsmState

  }


  /** Трейт для сборки состояния нахождения в состоянии ожидания сигнала EnsureMap с готовой картой на руках. */
  protected trait MapDetachedStateT extends IEnsureMapHandler {

    override def _handleEnsureMap(em: EnsureMap): Unit = {
      // Найти текущий контейнер карты.
      val sgContOpt = SGeoContent.find()

      val nextState = sgContOpt.fold [FsmState] {
        // Почему-то нет текущего контейнера
        warn( WarnMsgs.MAP_ELEM_MISSING_BUT_EXPECTED )
        _detachReattachFailedState

      } { sgContNew =>
        // Как и ожидалось, тег-контейнер карты на месте. Заменить его текущим готовым контейнером.
        val sd0 = _stateData
        sd0.detached.fold [FsmState] {
          // Should never happen. Почему-то в состоянии нет нужных данных.
          error( WarnMsgs.MAP_DETACH_STATE_INVALID )
          _detachReattachFailedState

        } { sgContDetached =>
          // Есть на руках detached-контейнер карты. Запихнуть его назад в DOM.
          sgContNew.replaceWith( sgContDetached )
          _stateData = sd0.copy(
            detached = None
          )
          _attachSuccessState
        }
      }
      become(nextState)

    }

    def _detachReattachFailedState: FsmState
    def _attachSuccessState: FsmState

  }

}
