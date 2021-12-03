package models.msc

import io.suggest.n2.node.MNode
import io.suggest.sc.MScApiVsn
import io.suggest.spa.SioPages
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
                              jsStateOpt    : Option[SioPages.Sc3] = None,
                              mainScreen    : SioPages.Sc3,
                            )

