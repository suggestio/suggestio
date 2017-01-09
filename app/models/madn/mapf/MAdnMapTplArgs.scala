package models.madn.mapf

import io.suggest.model.n2.node.MNode
import models.adv.form.IAdvForAdFormCommonTplArgs
import models.adv.price.{IAdvPricing, MAdvPricing}
import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.16 16:20
  * Description: Модель аргументов для шаблона [[views.html.lk.adn.mapf.AdnMapTpl]].
  */

trait IAdnMapTplArgs extends IAdvForAdFormCommonTplArgs {

  /** Текущий ADN-узел. */
  def mnode   : MNode

  /** Маппинг формы размещения узла. */
  def form    : Form[MAdnMapFormRes]

  /** Текущая стоимость размещения. */
  def price   : IAdvPricing

}


/** Дефолтовая реализация модели [[IAdnMapTplArgs]]. */
case class MAdnMapTplArgs(
  override val mnode      : MNode,
  override val form       : Form[MAdnMapFormRes],
  override val price      : MAdvPricing
)
  extends IAdnMapTplArgs
