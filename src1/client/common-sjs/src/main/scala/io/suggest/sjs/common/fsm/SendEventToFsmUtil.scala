package io.suggest.sjs.common.fsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:54
 * Description:
 */
object SendEventToFsmUtil {

  /**
   * Сборка частоиспользуемой функции отправки события в указанный FSM.
   * @param fsm Конечный автомат, принимающий сообщения.
   * @param model Модель сообщения, в которую будет завёрнут DOM event.
   * @tparam EventT Тип DOM event.
   * @return Функция-подписчик на события.
   */
  def f[EventT](fsm: SjsFsm, model: IFsmMsgCompanion[EventT]) = {
    {e: EventT =>
      fsm !! model(e)
    }
  }

}


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
    SendEventToFsmUtil.f(FSM, model)
  }

}
