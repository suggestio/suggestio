package models.mlk

import models.{MNode, blk}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 15:19
 * Description: Модели аргументов для вызова рендера шаблона lk.adn.nodeAdsTpl() и смежных.
 */

/** Интерфейс контейнера аргументов для шаблона _myAdsTpl. */
trait INodeAdsTplArgs {
  def mads        : Seq[blk.IRenderArgs]
  def mnode       : MNode
  def canAdv      : Boolean
}


/** Контейнер аргументов для шаблона nodeAdsTpl. */
case class MNodeAdsTplArgs(
  override val mnode        : MNode,
  override val mads         : Seq[blk.IRenderArgs],
  override val canAdv       : Boolean
)
  extends INodeAdsTplArgs
