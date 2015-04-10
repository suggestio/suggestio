package models.msc

import models.{GeoNodesLayer, MAdnNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:29
 * Description: Аргументы для рендера шаблона sc/geoNodesListTpl.
 */

trait NodeListRenderArgs extends SyncRenderInfoDflt {
  def nodeLayers: Seq[GeoNodesLayer]
  def currNode: Option[MAdnNode]
}

/** Враппер для [[NodeListRenderArgs]].  */
trait NodeListRenderArgsWrapper extends NodeListRenderArgs {
  def _nlraUnderlying: NodeListRenderArgs

  override def nodeLayers = _nlraUnderlying.nodeLayers
  override def currNode = _nlraUnderlying.currNode

  override def jsStateOpt: Option[ScJsState] = _nlraUnderlying.jsStateOpt
  override def syncRender = _nlraUnderlying.syncRender
  override def syncUrl(jsState: ScJsState) = _nlraUnderlying.syncUrl(jsState)
}
