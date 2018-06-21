package io.suggest.sc.m.grid

import io.suggest.sc.m.{ISc3Action, IScApiRespReason}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 10:42
  * Description: Diode-экшены плитки карточек.
  */
sealed trait IGridAction extends ISc3Action


/** Команда к запуску заливки данных в плитку.
  *
  * @param clean true - Очистить плитку перед добавлением полученных карточек.
  *              false - Дописать полученные карточки в конец текущей плитки.
  * @param silent Скрытый апдейт плитки: без явной прокрутки куда-то вверх при clean-релоаде.
  *               None - решить самостоятельно.
  *               true - скрытые изменения в плитке.
  */
case class GridLoadAds(clean          : Boolean,
                       ignorePending  : Boolean,
                       silent         : Option[Boolean] = None
                      )
  extends IGridAction with IScApiRespReason


/** Клик по карточке в плитке. */
case class GridBlockClick(nodeId: String) extends IGridAction with IScApiRespReason

/** Экшен скроллинга плитки. */
case class GridScroll(scrollTop: Double) extends IGridAction


/** Пересчитать конфиги плитки, возможно перестроив плитку.
  * Полезно для ранней реакции на изменение размеров экрана.
  */
case object GridReConf extends IGridAction

/** Выполнен скроллинг карточек. */
case object GridScrollDone extends IGridAction
