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
  * @param afterLoadFx Какой эффект запустить после успешной загрузки карточек.
  */
case class GridLoadAds(
                        clean          : Boolean,
                        ignorePending  : Boolean,
                        silent         : Option[Boolean] = None,
                        onlyMatching   : Option[MScNodeMatchInfo] = None,
                        afterLoadFx    : Option[Effect] = None,
                      )
  extends IGridAction
  with IScApiRespReason


/** Клик по карточке в плитке.
  *
  * @param gridKey Указатель на кликнутую карточку.
  *                Может отсутствовать в дереве плитки, и тогда используется nodeId.
  * @param adId id фокусируемой рекламной карточки, если gridPtr отсутствует.
  * @param noOpen Не раскрывать указанную карточку, а только проскроллить к ней.
  */
case class GridBlockClick(
                           gridPath   : Option[List[GridAdKey_t]],
                           gridKey    : Option[GridAdKey_t],
                           adId       : Option[String]    = None,
                           noOpen     : Boolean           = false,
                         )
  extends IGridAction
  with IScApiRespReason

/** Экшен скроллинга плитки. */
case object GridScroll extends IGridAction

/** Экшен отправки в очередь экшена, который будет запущен после ближайшего обновления плитки.
  * Используется, когда во время обновления плитки/индекса возникла необходимость снова её дообновить.
  */
case class GridAfterUpdate( effect: Effect ) extends IGridAction
