package models.adv.geo.tag

import io.suggest.bill.MGetPriceResp
import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.form.IAdvForAdFormCommonTplArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.geo.AdvGeoForAdTpl]].
  */
trait IForAdTplArgs extends IAdvForAdCommonTplArgs with IAdvForAdFormCommonTplArgs {

  /**
    * Сериализованное состояние js-react-diode-формы.
    * Изначально тут был boopickle + base64 с инстансом MFormS внутри.
    */
  def formState    : String

}


case class MForAdTplArgs(
                          override val mad              : MNode,
                          override val producer         : MNode,
                          override val price            : MGetPriceResp,
                          override val formState        : String
                        )
  extends IForAdTplArgs
