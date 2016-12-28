package io.suggest.lk.router

import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.scalajs.js.{Dictionary, Any}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 11:57
  * Description: Интерфейс контроллер jsRouter'а с url экшенов LkAdvGeoTag контроллера.
  */

@js.native
sealed trait LkAdvGeoCtl extends js.Object {

  /** Роута постинга добавления нового тега. */
  def tagEditorAddTag(): Route = js.native

  /* Роута запроса рассчета стоимости текущего размещения. */
  //def getPriceSubmit(adId: String): Route = js.native

  /** Роута для поиска тегов. */
  def tagsSearch(args: Dictionary[Any]): Route = js.native
  /** Роута для поиска тегов  */
  def tagsSearch2(args: Dictionary[Any]): Route = js.native

  /** Роута для запроса ценника текущего размещения. */
  def getPriceSubmit(adId: String): Route = js.native

  /** Роута для итогового сабмита формы. */
  def forAdSubmit(adId: String): Route = js.native

  /** Роута для получения GeoJson карты ресиверов adv-geo. */
  def advRcvrsMap(adId: String): Route = js.native

  /** Роута получения содержимого попапа узла географической карты. */
  def rcvrMapPopup(adId: String, nodeId: String): Route = js.native

  /** Роута получения карты текущих георазмещений. */
  def currGeoAdvsMap(adId: String): Route = js.native

}
