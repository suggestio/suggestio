package io.suggest.sc.grid.m

import io.suggest.grid.build.GridBuildRes_t
import io.suggest.sc.m.ISc3Action
import io.suggest.sc.sc3.MSc3Resp

import scala.util.Try

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
  */
case class GridLoadAds(clean: Boolean, ignorePending: Boolean) extends IGridAction


/** Ответ сервера по поводу запрошенных ранее карточек.
  *
  * @param evidence Исходный экшен [[GridLoadAds]].
  * @param startTime Значение Pot Pending.startTime
  * @param resp Результат исполнения запроса.
  * @param limit Значение limit в исходном реквесте.
  */
case class GridLoadAdsResp(
                            evidence  : GridLoadAds,
                            startTime : Long,
                            resp      : Try[MSc3Resp],
                            limit     : Int
                          )
  extends IGridAction


/** Сигнал о получении данных по построенной плитке.
  *
  * @param res Результат работы GridBuilder'а.
  */
case class HandleGridBuildRes(res: GridBuildRes_t) extends IGridAction

/** Клик по карточке в плитке. */
case class GridBlockClick(nodeId: String) extends IGridAction


/** Экшен скроллинга плитки. */
case class GridScroll(scrollTop: Double) extends IGridAction
