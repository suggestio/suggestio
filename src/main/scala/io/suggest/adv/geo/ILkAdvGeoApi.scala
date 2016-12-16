package io.suggest.adv.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.16 18:46
  * Description: autowire API для общения между формой и API-контроллером на стороне сервера.
  */
trait ILkAdvGeoApi {

  /** Получить содержимое попапа для ресивера в контексте текущей карточки. */
  def rcvrPopup(adId: String, rcvrId: String): MRcvrPopupResp

}
