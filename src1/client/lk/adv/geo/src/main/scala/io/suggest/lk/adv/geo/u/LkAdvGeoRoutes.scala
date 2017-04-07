package io.suggest.lk.adv.geo.u

import io.suggest.lk
import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 21:46
  * Description: HTTP-routes до контроллера LkAdvGeo на стороне сервера.
  */
@js.native
sealed trait LkAdvGeoRoutes extends js.Object {

  /** Объект с роутами серверного контроллера LkAdvGeo. */
  val LkAdvGeo: LkAdvGeoCtl = js.native

}


/** Поддержка приведения ЛК js-роутера к js-роутеру ЛК-страницы гео-размещения. */
object LkAdvGeoRoutes {
  implicit def apply(lkJsRoutes: lk.router.Controllers): LkAdvGeoRoutes = {
    lkJsRoutes.asInstanceOf[LkAdvGeoRoutes]
  }
}


/** Description: Интерфейс контроллер jsRouter'а с роутами экшенов LkAdvGeoTag контроллера. */
@js.native
sealed trait LkAdvGeoCtl extends js.Object {

  /** Роута постинга добавления нового тега. */
  def tagEditorAddTag(): Route = js.native

  /** Роута для поиска тегов  */
  def tagsSearch2(args: js.Dictionary[js.Any]): Route = js.native


  /** Роута для запроса ценника текущего размещения. */
  def getPriceSubmit(adId: String): Route = js.native

  /** Роута для получения детализированной информации по стоимости размещения какого-то элемента. */
  def detailedPricing(adId: String, itemIndex: Int): Route = js.native

  /** Роута для итогового сабмита формы. */
  def forAdSubmit(adId: String): Route = js.native


  /** Роута для получения GeoJson карты ресиверов adv-geo. */
  def advRcvrsMap(adId: String): Route = js.native

  /** Роута получения содержимого попапа узла географической карты. */
  def rcvrMapPopup(adId: String, nodeId: String): Route = js.native


  /** Роута получения карты текущих георазмещений. */
  def existGeoAdvsMap(adId: String): Route = js.native

  /** Роута для получения содержимого попапа над указанной областью георазмещения. */
  def existGeoAdvsShapePopup(itemId: Double): Route = js.native

}
