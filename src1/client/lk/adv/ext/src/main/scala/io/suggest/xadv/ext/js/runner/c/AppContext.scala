package io.suggest.xadv.ext.js.runner.c

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 21:40
 * Description: AppContext - доступный адаптерам контекст, позволяющий им получить доступ
 * к приложению и его компонентам.
 */

trait IAppContext {

  /** Менеджер очереди попапов. */
  def popupQueue: IPopupQueue

}


/** Дефолтовая реализация AppContext. */
class AppContextImpl extends IAppContext {

  override val popupQueue: IPopupQueue = new PopupQueueImpl

}
