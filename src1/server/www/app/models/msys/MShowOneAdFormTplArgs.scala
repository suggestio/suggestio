package models.msys

import io.suggest.n2.node.MNode
import models.blk
import models.msc.OneAdRenderVariant
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.11.15 18:08
  * Description: Модель-контейнер параметров вызова шаблона [[views.html.sys1.market.ad.one.argsFormTpl]].
  */

trait IShowOneAdFormPageTplArgs extends IShowOneAdFormTplArgs {

  /** Рекламная карточка. */
  def mad: MNode

  /** Текущий узел (продьюсер), если известен. */
  def nodeOpt: Option[MNode]

  override def madId = mad.id.get

}


/** Модель аргументов для непосредственного шаблона формы [[views.html.sys1.market.ad.one._oneAdQsFormTpl]]. */
trait IShowOneAdFormTplArgs {

  /** id рекламной карточки. */
  def madId: String

  /** Маппинг формы аргументов рендера. */
  def qf: Form[blk.OneAdQsArgs]

  /** Ad rendering variant. */
  def rvar: OneAdRenderVariant

}


/** Дефолтовая реализация модели [[IShowOneAdFormPageTplArgs]]. */
case class MShowOneAdFormTplArgs(
  override val mad      : MNode,
  override val rvar     : OneAdRenderVariant,
  override val qf       : Form[blk.OneAdQsArgs],
  override val nodeOpt  : Option[MNode]
)
  extends IShowOneAdFormPageTplArgs
