package io.suggest.sjs.common.fsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:54
 * Description:
 */
trait SendEventToFsmUtil {

  /** Доступ к статическому FSM. */
  protected def FSM: SjsFsm

  /**
   * Сборка фунцкии для отправки DOM-события в ScFsm, заворачивая его в соотв.контейнер.
   * @param model Компаньон модели контейнера.
   * @tparam EventT Тип заворачиваемого события.
   * @return Функция, пригодная для повешивания в качестве листенера.
   */
  protected def _sendEventF[EventT](model: IFsmMsgCompanion[EventT]) = {
    {e: EventT =>
      FSM !! model(e)
    }
  }

}
