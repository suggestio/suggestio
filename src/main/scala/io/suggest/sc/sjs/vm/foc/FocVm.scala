package io.suggest.sc.sjs.vm.foc

import io.suggest.fsm.{AbstractFsmUtil, AbstractFsm}
import io.suggest.sc.sjs.m.msrv.foc.find.IMFocAds
import io.suggest.sc.sjs.vm.foc.fsm.msg._
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: ViewModel для focused-выдачи. Модель проецирует свои вызовы на состояние DOM,
 * а так же является конечным автоматом со своим внутренним состоянием.
 * FSM создает своё состояние при входе в focused-выдачи и разрушает при выходе.
 */
object FocVm extends AbstractFsm with SjsLogger {

  override type Receive = PartialFunction[IFsmMessage, Unit]

  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = PartialFunction.empty

  override protected val unexpectedReceiver: Receive = {
    case other =>
      error("wtf?: " + other)
  }

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }

  /** Контейнер с данными внутреннего FSM-состояния focused-выдачи. */
  override protected var _state: FsmState = new UnfocusedState



  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    ???
  }


  /** Состояние, когда focused-выдача отсутсвует и скрыта вообще. */
  protected class UnfocusedState extends FsmState {
    override def receiverPart: Receive = {
      // Сигнал к открытию focused-выдачи. Нужно создать состояние и запустить фокусировку на индексе 0.
      case GoTo(index) =>
        val stateData = FocVmState()
        become(new NeedToFocusState(index, stateData))

      // Сигнал к расфокусировке, но выдача уже закрыта. Игнорим.
      case Close =>
    }
  }

  /** Состояние подготовки к фокусировки на указанном индексе.
    * Здесь происходит поиск принятие решений по дальнейшим действиям. */
  protected class NeedToFocusState(targetIndex: Int, stateData: FocVmState) extends FsmState {
    override def receiverPart: PartialFunction[IFsmMessage, Unit] = {
      ???
    }
  }

  protected class WaitForFadsState(nextIndex: Int, stateData: FocVmState) extends FsmState {
    override def receiverPart: Receive = {
      case FadsReceived(fads) =>
        val fadsIter2 = fads.focusedAdsIter.map { fad =>
          fad.index -> FocAdVm(fad)
        }
        val stateData1 = stateData.copy(
          ads           = stateData.ads ++ fadsIter2,
          loadedCount   = stateData.loadedCount + fads.fadsCount,
          totalCount    = Some(fads.totalCount)
        )
        // TODO Переключиться на следующее состояние.
        ???
    }
  }

}


/**
 * Экземпляр контейнера данных состояния FSM Focused ViewModel.
 * @param rootCont Корневой контейнер focused-выдачи.
 * @param ads Аккамулятор уже загруженных с сервера focused-карточек.
 * @param totalCount Общее кол-во карточек со всех возможных выборов в рамках задачи.
 *                   Если None, значит точное кол-во пока не известно.
 */
case class FocVmState(
  rootCont    : FocRootContainerVm = FocRootContainerVm(),
  ads         : Map[Int, FocAdVm] = Map.empty,
  currIndex   : Option[Int] = None,
  loadedCount : Int = 0,
  totalCount  : Option[Int] = None
)
