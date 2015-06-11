package models.msc

import models.{blk, MAdnNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:25
 * Description:
 */

trait IAdBodyTplArgs {
  def brArgs    : blk.IRenderArgs
  def producer  : MAdnNode
  def adsCount  : Int
  def index     : Int
}

/** Дефолтовая реализация [[IAdBodyTplArgs]]. */
case class AdBodyTplArgs(
  override val brArgs    : blk.IRenderArgs,
  override val producer  : MAdnNode,
  override val adsCount  : Int,
  override val index     : Int
)
  extends IAdBodyTplArgs
