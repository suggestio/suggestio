package io.suggest.sc.sjs.c.plat.dev

import io.suggest.sc.sjs.c.plat.PlatformFsmStub
import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sjs.common.fsm.signals.PlatformReady

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 16:43
  * Description: Состояния накапливания listen-сигналов до наступления сигнала готовности платформы.
  */
trait NotReady extends PlatformFsmStub { me =>

  /** Трейт для сборки состояния ожидания платформы к готовности к работе. */
  trait NotReadyStateT extends FsmState {

    /** Внутренний аккамулятор событий, которые надо повторно обработать после наступления ready в следующем состоянии. */
    private var _awaitingAccRev: List[PlatEventListen] = Nil


    /** Подписаться на события готовности платформы к работе. */
    def _subscribePlatformReady(): Unit

    /** Отписаться от событий готовности платформы к работе. */
    def _unSubscribePlatformReady(): Unit


    override def afterBecome(): Unit = {
      super.afterBecome()

      // Подписаться на device ready при заходе в состояние.
      _subscribePlatformReady()
    }


    override def receiverPart: Receive = {
      // Сигнал подписки на какие-то платформенные события.
      case pel: PlatEventListen if pel.subscribe =>
        _handlePlatformListen(pel)

      // Самый ожидаемый сигнал: нижележащая платформа сообщает о своей готовности к работе.
      case pr: PlatformReady =>
        _handlePlatformReady(pr)
    }


    /** Реакция на platform-listen-события от клиентуры. */
    def _handlePlatformListen(pel: PlatEventListen): Unit = {
      // Если подписка на device ready, то добавить в карту подписчиков.
      _awaitingAccRev ::= pel
    }


    /** Пришёл сигнал о готовности платформы к работе. */
    protected def _handlePlatformReady(pr: PlatformReady): Unit = {
      val eDRdy = PlatformEvents.E_DEVICE_READY
      val awaitingAcc = _awaitingAccRev.reverse

      // Нужно уведомить всех ожидающих готовности платформы, если они есть:
      val filterF = { pel: PlatEventListen =>
        pel.event == eDRdy
      }
      _broadcastToAll(
        listeners = awaitingAcc.iterator
          .filter(filterF)
          .map(_.listener),
        signal    = pr
      )

      // Отказаться от дальнейших уведомлений по готовности платформы.
      _unSubscribePlatformReady()

      // Перейти на след состояние.
      become(_nextState)

      // Запустить в новом состоянии обработку накопившихся listen-сигналов в порядки их получения.
      for {
        pel <- awaitingAcc
          .iterator
          .filterNot(filterF)
      } {
        me ! pel
      }
    }


    /** На какое состояние переключаться, когда платформа станет готова? */
    protected def _nextState: FsmState

  }

}
