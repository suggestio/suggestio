package models.msys

import io.suggest.model.n2.extra.domain.MDomainExtra
import models.MNode
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.16 16:18
  * Description: Общий интерфейс моделей контейнера аргументов для вызова шаблона [[views.html.sys1.domains._nodeDomainsFormTpl]].
  */

trait ISysNodeDomainFormTplArgs {

  /** Текущий узел N2. */
  def mnode   : MNode

  /** Маппинг текущей формы. */
  def form    : Form[MDomainExtra]

}


/** Модель контейнера аргументов шаблона добавления домена к узлу [[views.html.sys1.domains.createNodeDomainTpl]]. */
case class MSysNodeDomainCreateFormTplArgs(
  override val mnode   : MNode,
  override val form    : Form[MDomainExtra]
)
  extends ISysNodeDomainFormTplArgs


/** Модель контейнера аргументов шаблона редактирования домена у узла [[views.html.sys1.domains.editNodeDomainTpl]].  */
case class MSysNodeDomainEditFormTplArgs(
  mdx                  : MDomainExtra,
  override val mnode   : MNode,
  override val form    : Form[MDomainExtra]
)
  extends ISysNodeDomainFormTplArgs
