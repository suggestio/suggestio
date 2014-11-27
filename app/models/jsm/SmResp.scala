package models.jsm

import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.ym.model.MAdnNode
import play.api.libs.json._
import play.api.mvc.Call

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


trait Action extends SmJsonResp{
  def action: String

  override def toJsonAcc: FieldsJsonAcc = {
    "action" -> JsString(action)  ::  super.toJsonAcc
  }
}

trait Blocks extends SmJsonResp {
  def blocks: Seq[JsString]
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _blocks = blocks
    if (_blocks.nonEmpty)
      "blocks" -> JsArray(_blocks) :: acc0
    else
      acc0
  }
}

trait TileAdsResp extends Action with Blocks

case class FindAdsResp(blocks: Seq[JsString]) extends TileAdsResp {
  override def action = "findAds"
}
case class SearchAdsResp(blocks: Seq[JsString]) extends TileAdsResp {
  override def action = "searchAds"
}

trait HtmlOpt extends SmJsonResp {
  def htmlOpt: Option[JsString]

  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _htmlOpt = htmlOpt
    if (_htmlOpt.nonEmpty)
      "html" -> _htmlOpt.get :: acc0
    else
      acc0
  }
}

trait FocusedAdsResp extends Action with HtmlOpt with Blocks

/**
 * Навигация в карточках продьюсера. Тут логика focused ads.
 * @param htmlOpt Нулевой отображаемый блок должен быть уже отрендерен сюда.
 * @param blocks Отрендеренные блоки за экраном.
 */
case class ProducerAdsResp(htmlOpt: Option[JsString], blocks: Seq[JsString]) extends FocusedAdsResp {
  override def action = "producerAds"
}


trait IsGeo extends SmJsonResp {
  def isGeo: Boolean
  override def toJsonAcc: FieldsJsonAcc = {
    "is_geo" -> JsBoolean(isGeo) :: super.toJsonAcc
  }
}

trait CurrAdnId extends SmJsonResp {
  def currAdnId: Option[String]

  override def toJsonAcc: FieldsJsonAcc = {
    val caiJson = currAdnId.fold [JsValue] (JsNull) { cai => JsString(cai) }
    "curr_adn_id" -> caiJson  ::  super.toJsonAcc
  }
}

/**
 * showcase index ответ.
 * @param html верстка выдачи.
 * @param isGeo значение флага isGeo.
 * @param currAdnId Текущий id узла, к которому относится отображаемая выдача.
 */
case class ScIndexResp(html: JsString, isGeo: Boolean, currAdnId: Option[String])
extends Action with HtmlOpt with IsGeo with CurrAdnId {
  override def action = "showcaseIndex"
  override def htmlOpt: Option[JsString] = Some(html)
}


trait Timestamp extends SmJsonResp {
  def timestamp: Long
  override def toJsonAcc: FieldsJsonAcc = {
    "timestamp" -> JsNumber(timestamp) :: super.toJsonAcc
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
    "nodes" -> nodesListHtml :: super.toJsonAcc
  }
}

trait NodeNameId extends SmJsonResp {
  def adnNodeNameIdFn: String
  def adnNode: MAdnNode
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _node = adnNode
    val v = JsObject(Seq(
      "name" -> JsString(_node.meta.name),
      "_id"  -> JsString(_node.id getOrElse "")
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
case class NodeListResp(adnNode: MAdnNode, nodesListHtml: JsString, timestamp: Long, status: String)
extends NodeNameId with NodeListHtml with Status with Timestamp with Action {
  override def adnNodeNameIdFn = "first_node"
  override def action = "findNodes"
}


trait ColorOpt extends SmJsonResp {
  def colorOpt: Option[String]
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val v = if (colorOpt.isDefined) JsString(colorOpt.get) else JsNull
    "color" -> v :: acc0
  }
}

trait LogoUrl extends SmJsonResp {
  def logoUrlOpt: Option[Call]
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val v = if (logoUrlOpt.isDefined) JsString(logoUrlOpt.get.url) else JsNull
    "logo_src" -> v :: acc0
  }
}

/**
 * Ответ на запрос выдачи к ctl.MS.nodeData().
 * @param colorOpt Данные по основному цвету.
 * @param logoUrlOpt Ссылка на логотип.
 */
case class NodeDataResp(colorOpt: Option[String], logoUrlOpt: Option[Call])
extends LogoUrl with ColorOpt with Action {
  override def action = "setData"
}

