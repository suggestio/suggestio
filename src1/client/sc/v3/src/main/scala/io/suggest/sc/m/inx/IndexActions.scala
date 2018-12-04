package io.suggest.sc.m.inx

import io.suggest.sc.m.{ISc3Action, IScIndexRespReason}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:41
  * Description: Diode-экшены для index'а.
  */

/** Интерфейс-маркер для index-экшенов. */
sealed trait IIndexAction extends ISc3Action

/** Дёрнуть индекс с сервера и накатить. */
case class GetIndex( switchCtx: MScSwitchCtx )
  extends IIndexAction with IScIndexRespReason


/** Нажатие на экран приветствия. */
case object WcClick extends IIndexAction

/** Срабатывание таймера автоматического переключения фазы welcome.
  * @param timestamp Таймштамп-отметка, для проверки актуальности сработавшего таймера.
  */
case class WcTimeOut(timestamp: Long) extends IIndexAction


/** Команда к ребилду текущего инстанса ScCss. */
case object ScCssReBuild extends IIndexAction


/** Управление открытием-сокрытием какой-либо боковой панели.
  *
  * @param bar Панель, которую надо открыть/закрыть.
  * @param open Открыть панель?
  */
case class SideBarOpenClose(bar: MScSideBar, open: Boolean) extends IIndexAction


/** Отмена переключения индекса выдачи в новую локацию. */
case object CancelIndexSwitch extends IIndexAction

/** Подтверждение переключения индекса в новую локацию. */
case object ApproveIndexSwitch extends IIndexAction


/** Команда перехода на предыдущий узел. */
case object GoToPrevIndexView extends IIndexAction with IScIndexRespReason