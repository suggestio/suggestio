package models.adv.direct

import io.suggest.bill.MGetPriceResp
import io.suggest.model.n2.node.MNode
import models.adv.IAdvForAdCommonTplArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.15 14:06
  * Description: Модель контейнера аргументов вызова шаблона [[views.html.lk.adv.direct.advForAdTpl]].
  */
trait IAdvForAdTplArgs extends IAdvForAdCommonTplArgs {

  /** Данные формы размещения. */
  def formArgs  : IAdvFormTplArgs

}


case class MAdvForAdTplArgs(
  override val mad       : MNode,
  override val producer  : MNode,
  override val formArgs  : IAdvFormTplArgs,
  override val price     : MGetPriceResp
)
  extends IAdvForAdTplArgs
