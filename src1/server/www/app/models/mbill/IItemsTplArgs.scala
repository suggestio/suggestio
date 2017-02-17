package models.mbill

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.MNode
import models.blk.IRenderArgs
import models.im.MImgT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.17 16:40
  * Description: Реинкарнация модели данных для рендера списков item'ов в ЛК.
  * @see [[views.html.lk.billing.order._ItemsTpl]].
  */
trait IItemsTplArgs {

  /**
    * Список узлов, которые являются основой для рендера списка покупок.
    * Обычно - это карточки, но не обязательно.
    */
  def itemNodes   : Seq[MNode]

  /** Карта логотипов. */
  def node2logo   : Map[String, MImgT]

  /** Карта аргументов рендера карточек. */
  def node2brArgs : Map[String, IRenderArgs]

  /** Карта item'ов для одного узла в списке item-узлов. */
  def node2items  : Map[String, Seq[MItem]]

}
