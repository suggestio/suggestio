package models.adv.geo.tag

import models.MNode
import models.adv.IAdvForAdCommonTplArgs
import models.adv.form.IAdvForAdFormCommonTplArgs
import models.adv.price.IAdvPricing
import play.api.libs.json.JsValue

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 15:05
  * Description: Модель аргументов шаблона [[views.html.lk.adv.geo.AdvGeoForAdTpl]].
  */
trait IForAdTplArgs extends IAdvForAdCommonTplArgs with IAdvForAdFormCommonTplArgs {

  /** Экземпляр маппинга формы размещения карточки в теге с географией. */
  def form          : AgtForm_t

  /** Текущие размещения карточки. */
  def currAdvsJson  : JsValue

  /**
    * Сериализованное состояние js-react-diode-формы.
    * Изначально тут был boopickle + base64 с инстансом [[io.suggest.adv.geo.MRoot]] внутри.
    */
  def formState    : String

}


case class MForAdTplArgs(
  override val mad              : MNode,
  override val producer         : MNode,
  override val form             : AgtForm_t,
  override val price            : IAdvPricing,
  override val currAdvsJson     : JsValue,
  override val formState        : String
)
  extends IForAdTplArgs
