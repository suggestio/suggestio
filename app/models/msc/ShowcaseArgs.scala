package models.msc

import controllers.routes
import models._
import models.blk.SzMult_t
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 * 2014.nov.11: "Showcase" и "SMShowcase" в названиях классов сокращены до "Sc"
 */

trait SyncRenderInfo {
  def jsStateOpt: Option[ScJsState]
  def syncRender: Boolean = jsStateOpt.isDefined
  def syncUrl(jsState: ScJsState): String = routes.MarketShowcase.syncGeoSite(jsState).url
}
trait SyncRenderInfoDflt extends SyncRenderInfo {
  override def jsStateOpt: Option[ScJsState] = None
}


/** Экземпляр отрендернной рекламной карточки*/
trait RenderedAdBlock {
  def mad: MAd
  def rendered: Html
}
case class RenderedAdBlockImpl(mad: MAd, rendered: Html) extends RenderedAdBlock


/** Настройки рендера плитки на клиенте. */
case class TileArgs(szMult: SzMult_t, colsCount: Int)
