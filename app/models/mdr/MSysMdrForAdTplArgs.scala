package models.mdr

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.MNode
import models.{MEdge, blk}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.16 10:38
  * Description: Аргументы для вызова шаблона модерации одной рекламной карточки.
  *
  * @see [[views.html.sys1.mdr.forAdTpl]]
  */
trait ISysMdrForAdTplArgs {

  /** Параметры рендера текущей рекламной карточки. */
  def brArgs: blk.RenderArgs

  /** Карта узлов, которые упомянуты в других аргументах. */
  def mnodesMap: Map[String, MNode]

  /** Модерируемы товары/узлуги. */
  def mitems: Seq[MItem]

  /** Данные по бесплатным размещениям карточки */
  def freeAdvs: Seq[MEdge]

  /** id продьюсера текущей рекламной карточки, если есть. */
  def producerIdOpt: Option[String]

}


/** Дефолтовая реализация [[ISysMdrForAdTplArgs]]. */
case class MSysMdrForAdTplArgs(
  override val brArgs         : blk.RenderArgs,
  override val mnodesMap      : Map[String, MNode],
  override val mitems         : Seq[MItem],
  override val freeAdvs       : Seq[MEdge],
  override val producerIdOpt  : Option[String]
)
  extends ISysMdrForAdTplArgs
