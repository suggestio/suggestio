package io.suggest.sc.grid.m

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
case class GridLoadAds(clean: Boolean) extends IGridAction


/** Ответ сервера по поводу запрошенных ранее карточек.
  *
  * @param evidence Исходный экшен [[GridLoadAds]].
  * @param startTime Значение Pot Pending.startTime
  * @param resp Результат исполнения запроса.
  */
case class GridLoadAdsResp(
                            evidence  : GridLoadAds,
                            startTime : Long,
                            resp      : Try[MSc3Resp]
                          )
  extends IGridAction

