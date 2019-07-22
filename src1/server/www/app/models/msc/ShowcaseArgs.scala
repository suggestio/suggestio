package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 * 2014.nov.11: "Showcase" и "SMShowcase" в названиях классов сокращены до "Sc"
 */

trait SyncRenderInfoDflt {
  def syncRender: Boolean = jsStateOpt.isDefined
  def jsStateOpt: Option[ScJsState] = None
}
