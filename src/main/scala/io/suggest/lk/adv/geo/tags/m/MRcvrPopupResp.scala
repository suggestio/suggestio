package io.suggest.lk.adv.geo.tags.m

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import io.suggest.adv.geo.AdvGeoConstants.AdnNodes.Popup._
import io.suggest.sjs.dt.period.m.IDatesPeriodInfo

import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.12.16 15:57
  * Description: Модель ответа с реквеста данных для попапа ресивера.
  */

/** Интерфес JSON-ответа сервера. */
@js.native
sealed trait IRcvrPopupResp extends js.Object {

  @JSName( GROUPS_FN )
  val groups    : js.Array[IRcvrPopupGroup] = js.native

}


/** JSON данных одной группы в ответе сервера [[IRcvrPopupResp]]. */
@js.native
sealed trait IRcvrPopupGroup extends js.Object {

  @JSName( NAME_FN )
  val name      : UndefOr[String] = js.native

  @JSName( GROUP_ID_FN )
  val groupId   : UndefOr[String] = js.native

  @JSName( NODES_FN )
  val nodes     : js.Array[IRcvrPopupNode] = js.native

}


/** JSON с данными одного узла в ответе [[IRcvrPopupResp]] в рамках одной группы [[IRcvrPopupGroup]]. */
@js.native
sealed trait IRcvrPopupNode extends js.Object {

  @JSName( NODE_ID_FN )
  val nodeId          : String = js.native

  @JSName( IS_CREATE_FN )
  val isCreate        : Boolean = js.native

  @JSName( CHECKED_FN )
  val checked         : Boolean = js.native

  @JSName( NAME_FN )
  val nameOpt         : UndefOr[String] = js.native

  @JSName( IS_ONLINE_NOW_FN )
  val isOnlineNow     : Boolean = js.native

  @JSName( INTERVAL_FN )
  val intervalOpt     : UndefOr[IDatesPeriodInfo] = js.native

}
