package models.mdr

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.node.MNode
import models.blk

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 10:38
  * Description: Аргументы для вызова шаблона модерации одной рекламной карточки.
  *
  * @see [[views.html.sys1.mdr.forAdTpl]]
  */

case class MSysMdrForAdTplArgs(
                                brArgs         : blk.RenderArgs,
                                mnodesMap      : Map[String, MNode],
                                mitemsGrouped  : Seq[(MItemType, Seq[MItem])],
                                freeAdvs       : Seq[MEdge],
                                producer       : MNode,
                                tooManyItems   : Boolean,
                                itemsCount     : Int,
                                freeMdrs       : Seq[MEdge]
                              ) {

  /** Исходная модерация была только для карточек, а тут -- расширение для узлов. */
  def mnode = brArgs.mad

  def adId = mnode.id.get

  /** Есть ли хоть один положительный отзыв модератора? */
  def hasPositiveMdr: Boolean = {
    freeMdrs
      .flatMap(_.info.flag)
      .contains(true)
  }

}

