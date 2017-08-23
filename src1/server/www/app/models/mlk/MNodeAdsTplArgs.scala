package models.mlk

import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.model.n2.node.MNode
import models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.15 15:19
 * Description: Модели аргументов для вызова рендера шаблона lk.adn.nodeAdsTpl() и смежных.
 */

/** Интерфейс контейнера аргументов для шаблона _myAdsTpl. */
trait INodeAdsTplArgs {
  def mads        : Seq[INodeAdInfo]
  def mnode       : MNode
  def canAdv      : Boolean
}


/** Контейнер аргументов для шаблона nodeAdsTpl. */
case class MNodeAdsTplArgs(
  override val mnode        : MNode,
  override val mads         : Seq[INodeAdInfo],
  override val canAdv       : Boolean
)
  extends INodeAdsTplArgs


/** Трейт инфы об одной карточке. */
trait INodeAdInfo {

  val brArgs: blk.IRenderArgs
  def itemStatuses: Set[MItemStatus]

  def mad = brArgs.mad

  private def _hs(status: MItemStatus) = itemStatuses.contains( status )

  import MItemStatuses._
  def isOnline    = _hs( Online )
  def isOffline   = _hs( Offline )
  def isApproved  = isOnline || isOffline
  def isReq       = _hs( AwaitingMdr )
  def isBusy      = isApproved || isReq

}

object MNodeAdInfo {
  /** Статусы, поддерживаемые API модели.
    * См. методы isOnline, isOffline и прочие в [[INodeAdInfo]]. */
  def statusesSupported = MItemStatuses.advBusy
}

/** Дефолтовая реализация модели [[INodeAdInfo]]. */
case class MNodeAdInfo(
  override val brArgs       : blk.IRenderArgs,
  override val itemStatuses : Set[MItemStatus]
)
  extends INodeAdInfo
