package models.msys

import models.mdr.MdrSearchArgs
import models.{MNode, blk}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.10.15 13:10
 * Description: Модель аргументов для рендера шаблона [[views.html.sys1.mdr.freeAdvMdrTpl]],
 * содержащий данные о модерации бесплатно-размещенных карточек.
 */

trait ISysMdrFreeAdvsTplArgs {
  /** Начальные аргументы поиска карточек для модерации. */
  def args0         : MdrSearchArgs
  def mads          : Seq[blk.IRenderArgs]
  def prodsMap      : Map[String, MNode]
  def producerOpt   : Option[MNode]
}


case class MSysMdrFreeAdvsTplArgs(
  override val args0       : MdrSearchArgs,
  override val mads        : Seq[blk.IRenderArgs],
  override val prodsMap    : Map[String, MNode],
  override val producerOpt : Option[MNode]
)
  extends ISysMdrFreeAdvsTplArgs
