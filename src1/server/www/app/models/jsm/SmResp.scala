package models.jsm

import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import models.MNode
import play.api.libs.json._
import io.suggest.sc.ScConstants.Resp._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 14:07
 * Description: json response для выдачи, передаваемые в функцию siomart.receive_response().
 */

trait SmJsonResp {

  def toJsonAcc: FieldsJsonAcc = Nil
  def toJson: JsObject = JsObject(toJsonAcc)

}


trait Action extends SmJsonResp {
  def action: String

  override def toJsonAcc: FieldsJsonAcc = {
    ACTION_FN -> JsString(action)  ::  super.toJsonAcc
  }
}


// ------------------------------------------------------------------------------


trait Timestamp extends SmJsonResp {
  def timestamp: Long
  override def toJsonAcc: FieldsJsonAcc = {
    TIMESTAMP_FN -> JsNumber(timestamp) :: super.toJsonAcc
  }
}


trait Status extends SmJsonResp {
  def status: String
  override def toJsonAcc: FieldsJsonAcc = {
    "status" -> JsString(status) :: super.toJsonAcc
  }
}


trait NodeListHtml extends SmJsonResp {
  def nodesListHtml: JsString
  override def toJsonAcc: FieldsJsonAcc = {
    NODE_LIST_HTML_FN -> nodesListHtml :: super.toJsonAcc
  }
}

trait NodeNameId extends SmJsonResp {
  def adnNodeNameIdFn: String
  def adnNode: MNode
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val mnode = adnNode
    val v = JsObject(Seq(
      "name" -> JsString(mnode.meta.basic.name),
      "_id"  -> JsString(mnode.id getOrElse "")
    ))
    adnNodeNameIdFn -> v :: acc0
  }
}

/**
 * Ответ на запрос списка нод.
 * @param adnNode Текущий (следующий) узел.
 * @param nodesListHtml Отрендеренный список узлов.
 * @param timestamp Время запроса.
 * @param status Статус. Возможно, это бесполезное поле.
 */
case class NodeListResp(adnNode: MNode, nodesListHtml: JsString, timestamp: Long, status: String)
extends NodeNameId with NodeListHtml with Status with Timestamp with Action {
  override def adnNodeNameIdFn = "first_node"
  override def action = "findNodes"
}
