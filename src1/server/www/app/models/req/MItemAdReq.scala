package models.req

import io.suggest.mbill2.m.item.MItem
import models.MNode
import play.api.mvc.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 11:21
  * Description: Модель-контейнер данных реквестов с mad и mitem внутри одновременно.
  */
trait IItemAdReq[A]
  extends IItemReq[A]
  with IAdReq[A]


/** Дефолтовая реализация [[IItemAdReq]]. */
case class MItemAdReq[A](
  override val mitem    : MItem,
  override val mad      : MNode,
  override val user     : ISioUser,
  override val request  : Request[A]
)
  extends MReqWrap[A]
  with IItemAdReq[A]
