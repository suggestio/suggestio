package models.msc

import models._
import models.mext.tw.ICardArgs
import play.api.mvc.Call
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:24
 * Description: Контейнер для аргументов, передаваемых в sc/siteTpl.
 */

trait ScSiteArgs extends SyncRenderInfoDflt {
  /** Цвет оформления. */
  def bgColor       : String
  /** Адрес для showcase */
  def showcaseCall  : Call
  /** Текущая нода. Создавалась для генерации заголовка в head.title. */
  def nodeOpt       : Option[MAdnNode] = None
  /** Инлайновый рендер индексной страницы выдачи. В параметре содержится отрендеренный HTML. */
  def inlineIndex   : Option[Html] = None
  /** Закинуть сие в конец тега head. */
  def headAfter     : Option[Html] = None
  /** Метаданные для твиттера, если есть. */
  def twitterMeta   : Option[ICardArgs] = None

  // Имитируем поведение параметра, чтобы в будущем не рисовать костыли в коде шаблонов.
  def adnId   = nodeOpt.flatMap(_.id)
  def withGeo = adnId.isEmpty

  override def toString: String = {
    val sb = new StringBuilder(64)
    sb.append("bgColor=").append(bgColor).append('&')
      .append("showcaseCall=").append(showcaseCall).append('&')
      .append("syncRender=").append(syncRender).append('&')
    if (nodeOpt.isDefined)
      sb.append("node=").append(nodeOpt.get.idOrNull).append('&')
    if (adnId.isDefined)
      sb.append('&').append("adnId=").append(adnId)
    if (inlineIndex.isDefined)
      sb.append('&').append("inlineIndex=yes")
    sb.toString()
  }
}
/** Враппер для аргументов рендера "сайта" выдачи. */
trait ScSiteArgsWrapper extends ScSiteArgs {
  def _scSiteArgs: ScSiteArgs

  override def bgColor      = _scSiteArgs.bgColor
  override def showcaseCall = _scSiteArgs.showcaseCall
  override def nodeOpt      = _scSiteArgs.nodeOpt
  override def inlineIndex  = _scSiteArgs.inlineIndex
  override def headAfter    = _scSiteArgs.headAfter

  override def withGeo      = _scSiteArgs.withGeo
  override def toString     = _scSiteArgs.toString
  override def syncRender   = _scSiteArgs.syncRender
  override def twitterMeta  = _scSiteArgs.twitterMeta
}
