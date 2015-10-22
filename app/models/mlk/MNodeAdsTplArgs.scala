package models.mlk

import models.{MNode, MAdvI, blk}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 15:19
 * Description: Модели аргументов для вызова рендера шаблона lk.adn.nodeAdsTpl() и смежных.
 */

sealed trait IAdsListTplArgs {
  def mads        : Seq[blk.IRenderArgs]
  def ad2advMap   : Map[String, MAdvI]
}

/** Интерфейс контейнера аргументов для шаблона _readOnlyAdsListTpl. */
trait IAdsListTplArgsRo extends IAdsListTplArgs{
  def povAdnIdOpt : Option[String]
}

/** Интерфейс контейнера аргументов для шаблона _myAdsTpl. */
trait IAdsListTplArgsMy extends IAdsListTplArgs {
  def mnode       : MNode
  def canAdv      : Boolean
}


/** Интерфейс модели контейнера аргументов для шаблона nodeAdsTpl. */
trait INodeAdsTplArgs extends IAdsListTplArgsRo with IAdsListTplArgsMy {
  def isMyNode: Boolean
}


/** Контейнер аргументов для шаблона nodeAdsTpl. */
case class MNodeAdsTplArgs(
  override val mnode        : MNode,
  override val mads         : Seq[blk.IRenderArgs],
  override val isMyNode     : Boolean,
  override val povAdnIdOpt  : Option[String],
  override val canAdv       : Boolean,
  override val ad2advMap    : Map[String, MAdvI]
)
  extends INodeAdsTplArgs
