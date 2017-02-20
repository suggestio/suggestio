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

  /** Личный кабинет какого узла у нас сейчас? */
  def mnode       : MNode

  /**
    * Карта узлов по id, замешанных в рендере результата.
    * В первую очередь, тут должны быть ресиверы.
    */
  def nodesMap    : Map[String, MNode]

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


/** Дефолтовая реализация модели [[IItemsTplArgs]]. */
case class MItemsTplArgs(
                          override val mnode       : MNode,
                          override val nodesMap    : Map[String, MNode],
                          override val itemNodes   : Seq[MNode],
                          override val node2logo   : Map[String, MImgT],
                          override val node2brArgs : Map[String, IRenderArgs],
                          override val node2items  : Map[String, Seq[MItem]]
                        )
  extends IItemsTplArgs


/** wrap-реализация для модели [[IItemsTplArgs]]. */
trait IItemsTplArgsWrap extends IItemsTplArgs {

  def _underlying: IItemsTplArgs

  override def mnode        = _underlying.mnode
  override def nodesMap     = _underlying.nodesMap
  override def itemNodes    = _underlying.itemNodes
  override def node2logo    = _underlying.node2logo
  override def node2brArgs  = _underlying.node2brArgs
  override def node2items   = _underlying.node2items

}
