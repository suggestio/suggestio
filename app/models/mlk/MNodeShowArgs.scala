package models.mlk

import models.MAdnNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 12:08
 * Description: Модель передачи аргументов рендера в шаблон adnNodeShowTpl.
 */

trait INodeShowArgs {
  def mnode       : MAdnNode
  def isMyNode    : Boolean
  def povAdnIdOpt : Option[String]
}

case class MNodeShowArgs(
  override val mnode       : MAdnNode,
  override val isMyNode    : Boolean,
  override val povAdnIdOpt : Option[String]
)
  extends INodeShowArgs
