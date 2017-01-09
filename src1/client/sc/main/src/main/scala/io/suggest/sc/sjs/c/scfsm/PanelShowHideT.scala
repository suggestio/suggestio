package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.msc.MScSd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.16 17:15
  * Description: Общий код для статической поддержки боковых панелей выдачи.
  */
trait PanelShowHideT {

  /** Если панель отображается, то скрыть. Иначе -- показать. */
  def invert(sd0: MScSd): MScSd = {
    if (sd0.nav.panelOpened)
      hide(sd0)
    else
      show(sd0)
  }

  /**
    * Сокрытие панели.
    *
    * @param sd0 Начальное состояние, если есть.
    * @return Новое состояние FSM.
    */
  def hide(sd0: MScSd): MScSd

  /**
    * Код полной логики отображения панели.
    *
    * @param sd0 Исходные данные состояния ScFsm.
    * @return Обновлённые данные состояния ScFsm.
    */
  def show(sd0: MScSd): MScSd

}
