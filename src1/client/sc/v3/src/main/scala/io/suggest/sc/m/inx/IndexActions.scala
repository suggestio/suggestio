package io.suggest.sc.m.inx

import io.suggest.geo.MGeoPoint
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

/** Перезагрузить текущий индекс. */
case class ReGetIndex() extends IIndexAction

/** Сброс индекса. */
case object UnIndex extends IIndexAction



/** Команда к ребилду текущего инстанса ScCss. */
case object ScCssReBuild extends IIndexAction


/** Управление открытием-сокрытием какой-либо боковой панели.
  *
  * @param bar Панель, которую надо открыть/закрыть.
  * @param open Открыть панель?
  *             None означает инверсию состояния панели.
  */
case class SideBarOpenClose(bar: MScSideBar, open: Option[Boolean]) extends IIndexAction

/** Клик по элементу списка в InxSwitch.
  * @param nodeId None значит кнопку закрытия или автоматический выбор первого узла. */
case class IndexSwitchNodeClick( nodeId: Option[String] = None ) extends IIndexAction

/** Команда перехода на предыдущий узел. */
case object GoToPrevIndexView extends IIndexAction with IScIndexRespReason

/** Команда к запуску index-реакции на текущее состояние гео.карты. */
case class MapReIndex( rcvrId: Option[String], geoPoint: Option[MGeoPoint] ) extends IIndexAction with IScIndexRespReason


sealed trait IWelcomeAction extends ISc3Action

/** Нажатие на экран приветствия. */
case object WcClick extends IWelcomeAction

/** Срабатывание таймера автоматического переключения фазы welcome.
  * @param timestamp Таймштамп-отметка, для проверки актуальности сработавшего таймера.
  */
case class WcTimeOut(timestamp: Long) extends IWelcomeAction
