package models.adv.geo

import models.blk.IMadAndArgs
import models.{blk, MAd, MAdvReq}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 13:47
 * Description: Adv ReqInfo -- данные для рендера шаблона с запросами размещения карточек.
 */
trait IReqInfo extends IMadAndArgs {
  /** Запрос размещения. */
  def req     : MAdvReq
}


/** Дефолтовая реализация [[IReqInfo]]. */
case class ReqInfo(
  req     : MAdvReq,
  mad     : MAd,
  brArgs  : blk.RenderArgs
)
  extends IReqInfo
