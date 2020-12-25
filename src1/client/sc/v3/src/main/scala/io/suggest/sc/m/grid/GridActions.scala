package io.suggest.sc.m.grid

import diode.Effect
import io.suggest.jd.render.m.IGridAction
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.m.IScApiRespReason

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 10:42
  * Description: Diode-экшены плитки карточек.
  */

/** Команда к запуску заливки данных в плитку.
  *
  * @param clean true - Очистить плитку перед добавлением полученных карточек.
  *              false - Дописать полученные карточки в конец текущей плитки.
  * @param silent Скрытый апдейт плитки: без явной прокрутки куда-то вверх при clean-релоаде.
  *               None - решить самостоятельно.
  *               true - скрытые изменения в плитке.
  * @param onlyMatching Выборочный патчинг плитки по указанным предикатам, на основе которого были найдены карточки.
  *                    Предикат также определяет qs-поиска, и возвращённые сервером карточки замещают предыдущий
  *                    набор карточек, которые содержали в себе данный предикат в поле MScAdInfo().foundIn .
  */
case class GridLoadAds(
                        clean          : Boolean,
                        ignorePending  : Boolean,
                        silent         : Option[Boolean] = None,
                        onlyMatching   : Option[MScNodeMatchInfo] = None,
                      )
  extends IGridAction
  with IScApiRespReason


/** Клик по карточке в плитке. */
case class GridBlockClick(nodeId: String) extends IGridAction with IScApiRespReason

/** Экшен скроллинга плитки. */
case class GridScroll(scrollTop: Double) extends IGridAction

/** Экшен отправки в очередь экшена, который будет запущен после ближайшего обновления плитки.
  * Используется, когда во время обновления плитки/индекса возникла необходимость снова её дообновить.
  */
case class GridAfterUpdate( effect: Effect ) extends IGridAction
