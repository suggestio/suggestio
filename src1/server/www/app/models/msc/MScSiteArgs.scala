package models.msc

import io.suggest.n2.node.MNode
import io.suggest.sc.MScApiVsn
import io.suggest.sc.sc3.Sc3Pages
import play.twirl.api.Html

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:24
 * Description: Контейнер для аргументов, передаваемых в sc/siteTpl.
 */

final case class MScSiteArgs(
                              /** Текущая нода. Создавалась для генерации заголовка в head.title. */
                              nodeOpt       : Option[MNode] = None,
                              /** Инлайновый рендер индексной страницы выдачи. В параметре содержится отрендеренный HTML. */
                              /** Закинуть сие в конец тега head. */
                              headAfter     : Seq[Html],
                              /** В зависимости от версии API надо рендерить разные скрипты. */
                              scriptHtml    : Html,
                              /** Версия API выдачи. */
                              apiVsn        : MScApiVsn,
                              override val jsStateOpt: Option[Sc3Pages.MainScreen] = None,
                              override val syncRender: Boolean,
                            )
  extends SyncRenderInfoDflt
{

  // Пока оставлено тут, хотя это пережитки ScSyncSite, и наверное будет удалено.
  def inlineIndex   : Option[Html] = None

}

