package models.msc

import io.suggest.model.n2.node.MNode
import io.suggest.sc.MScApiVsn
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:24
 * Description: Контейнер для аргументов, передаваемых в sc/siteTpl.
 */

trait ScSiteArgs extends SyncRenderInfoDflt {

  /** Контейнер с цветами выдачи. */
  def scColors      : IScSiteColors
  /** Текущая нода. Создавалась для генерации заголовка в head.title. */
  def nodeOpt       : Option[MNode] = None
  /** Инлайновый рендер индексной страницы выдачи. В параметре содержится отрендеренный HTML. */
  def inlineIndex   : Option[Html] = None
  /** Закинуть сие в конец тега head. */
  def headAfter     : Traversable[Html] = Nil
  /** В зависимости от версии API надо рендерить разные скрипты. */
  def scriptHtml    : Html
  /** Версия API выдачи. */
  def apiVsn        : MScApiVsn

  // Имитируем поведение параметра, чтобы в будущем не рисовать костыли в коде шаблонов.
  def adnId   = nodeOpt.flatMap(_.id)

  override def toString: String = {
    val sb = new StringBuilder(64)
    sb.append("scColors=").append(scColors).append('&')
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

