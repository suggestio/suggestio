package models.adv.geo

import models.adv.MAdvReq
import models.blk
import models.blk.IBrArgs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 13:47
 * Description: Adv ReqInfo -- данные для рендера шаблона с запросами размещения карточек.
 */
trait IReqInfo extends IBrArgs {
  /** Запрос размещения. */
  def req     : MAdvReq
}


/** Дефолтовая реализация [[IReqInfo]]. */
case class ReqInfo(
  req     : MAdvReq,
  brArgs  : blk.RenderArgs
)
  extends IReqInfo
