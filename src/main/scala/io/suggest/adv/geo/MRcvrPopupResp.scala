package io.suggest.adv.geo

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:21
  * Description: Сериализуемые клиент-серверные модели содержимого попапа над ресивером на карте георазмещения.
  */

object MRcvrPopupResp {
  implicit val pickler: Pickler[MRcvrPopupResp] = {
    implicit val mrpgP = MRcvrPopupGroup.pickler
    generatePickler[MRcvrPopupResp]
  }
}
/** Модель ответа сервера на запрос попапа для ресивера. */
case class MRcvrPopupResp(groups: Seq[MRcvrPopupGroup])



/** JSON данных одной группы в ответе сервера [[MRcvrPopupResp]]. */
case class MRcvrPopupGroup(
                          nameOpt   : Option[String],
                          groupId   : Option[String],
                          nodes     : Seq[MRcvrPopupNode] )
object MRcvrPopupGroup {
  implicit val pickler: Pickler[MRcvrPopupGroup] = {
    implicit val rpnP = MRcvrPopupNode.pickler
    generatePickler[MRcvrPopupGroup]
  }
}


/** JSON с данными одного узла в ответе [[MRcvrPopupResp]] в рамках одной группы [[MRcvrPopupGroup]]. */
case class MRcvrPopupNode(
  nodeId          : String,
  isCreate        : Boolean,
  checked         : Boolean,
  nameOpt         : Option[String],
  isOnlineNow     : Boolean,
  dateRange       : Seq[MDateFormatted]
  //TODO intervalOpt     : Option[IDatesPeriodInfo]
)
object MRcvrPopupNode {
  implicit val pickler: Pickler[MRcvrPopupNode] = {
    implicit val mdfP = MDateFormatted.pickler
    generatePickler[MRcvrPopupNode]
  }
}


/** Отформатированная дата. */
object MDateFormatted {
  implicit val pickler: Pickler[MDateFormatted] = generatePickler[MDateFormatted]
}
case class MDateFormatted(
  date: String,
  dow: String
)
