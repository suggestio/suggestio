package models.msc

import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 10:04
 * Description: Аргументы для рендера тегов скрипта js-выдачи в sc/siteTpl.
 */
trait IScScriptRenderArgs {

  /** Георежим включен? Если да, то выдача должна запросить координаты у user-agent. */
  def withGeo: Boolean

  /** Ссылка для получения index.html выдачи. */
  def indexCall: Call

  /** id текущего узла, если есть. */
  def adnIdOpt: Option[String]

}


/** Дефолтовая реализация [[IScScriptRenderArgs]]. */
case class ScScriptRenderArgs(
  withGeo   : Boolean,
  indexCall : Call,
  adnIdOpt  : Option[String]
)
  extends IScScriptRenderArgs
