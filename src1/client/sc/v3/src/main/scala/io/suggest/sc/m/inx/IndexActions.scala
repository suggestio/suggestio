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

/** Дёрнуть индекс с сервера и накатить.
  *
  * @param withWelcome Требуется ли картинка приветствия?
  * @param focusedAdId Фокусироваться на id карточки.
  * @param retUserLoc Определять геолокацию юзера силами сервера?
  */
case class GetIndex(
                     withWelcome: Boolean,
                     geoIntoRcvr: Boolean,
                     override val focusedAdId: Option[String],
                     retUserLoc : Boolean,
                   )
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

