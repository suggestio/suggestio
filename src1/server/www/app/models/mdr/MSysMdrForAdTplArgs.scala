package models.mdr

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.node.MNode
import models.msc.IAdBodyTplArgs
import models.blk

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 10:38
  * Description: Аргументы для вызова шаблона модерации одной рекламной карточки.
  *
  * @see [[views.html.sys1.mdr.forAdTpl]]
  */
trait ISysMdrForAdTplArgs extends IAdBodyTplArgs {

  /** id модерируемой рекламной карточки. */
  def adId: String = brArgs.mad.id.get

  /** Параметры рендера текущей рекламной карточки. */
  def brArgs: blk.RenderArgs

  /** Карта узлов, которые упомянуты в других аргументах. */
  def mnodesMap: Map[String, MNode]

  /** Модерируемы товары/узлуги. */
  def mitemsGrouped: Seq[(MItemType, Seq[MItem])]

  /** Данные по бесплатным размещениям карточки */
  def freeAdvs: Seq[MEdge]

  /** true означает, что на экране отображаются не все платные размещения: их слишком много. */
  def tooManyItems: Boolean

  /** Общее кол-во item'ов для модерации для данной карточки. */
  def itemsCount: Int

  /** Данные об уже произведенных модерациях в отношении текущей версии карточки. */
  def freeMdrs: Seq[MEdge]

  /** Исходная модерация была только для карточек, а тут -- расширение для узлов. */
  def mnode = brArgs.mad

  /** Есть ли хоть один положительный отзыв модератора? */
  def hasPositiveMdr: Boolean = {
    freeMdrs
      .flatMap(_.info.flag)
      .contains(true)
  }

}


/** Дефолтовая реализация [[ISysMdrForAdTplArgs]]. */
case class MSysMdrForAdTplArgs(
  override val brArgs         : blk.RenderArgs,
  override val mnodesMap      : Map[String, MNode],
  override val mitemsGrouped  : Seq[(MItemType, Seq[MItem])],
  override val freeAdvs       : Seq[MEdge],
  override val producer       : MNode,
  override val tooManyItems   : Boolean,
  override val itemsCount     : Int,
  override val freeMdrs       : Seq[MEdge]
)
  extends ISysMdrForAdTplArgs
{

  override val adId = super.adId

  override def adsCount   = 1
  override def index      = 1
  override def is3rdParty = false

}

