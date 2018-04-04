package models.msc

import models.blk.SzMult_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 * 2014.nov.11: "Showcase" и "SMShowcase" в названиях классов сокращены до "Sc"
 */

trait ISyncRenderInfo {
  def jsStateOpt: Option[ScJsState]
  def syncRender: Boolean
  def syncUrl(jsState: ScJsState): String
}

trait SyncRenderInfo extends ISyncRenderInfo {
  def syncRender: Boolean = jsStateOpt.isDefined
  def syncUrl(jsState: ScJsState): String = {
    "NOT-IMPLEMENTED"
  }
}
trait SyncRenderInfoDflt extends SyncRenderInfo {
  override def jsStateOpt: Option[ScJsState] = None
}


/** Настройки рендера плитки на клиенте. */
case class TileArgs(szMult: SzMult_t, colsCount: Int)
