package models.jsm

import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import models.MNode
import models.msc.FocRenderResult
import play.api.libs.json._
import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.ScConstants.Focused.FOC_ANSWER_ACTION

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


trait HtmlOpt extends SmJsonResp {
  def htmlOpt: Option[JsString]

  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _htmlOpt = htmlOpt
    if (_htmlOpt.nonEmpty)
      HTML_FN -> _htmlOpt.get :: acc0
    else
      acc0
  }
}


/** Список focused-карточек по API v2. */
trait FocusedAdsT extends SmJsonResp {
  /** focused-карточки. */
  def ads: Seq[FocRenderResult]

  override def toJsonAcc: FieldsJsonAcc = {
    var acc = super.toJsonAcc
    val _ads = ads
    if (_ads.nonEmpty) {
      acc ::= FOCUSED_ADS_FN -> Json.toJson(_ads)
    }
    acc
  }
}


/** Аддон для поля общего кол-ва чего-то (карточек например) во всей выборке. */
trait TotalCountT extends SmJsonResp {
  /** Поле с ОБЩИМ кол-вом рекламных карточек во всей выборке. */
  def totalCount: Int

  override def toJsonAcc: FieldsJsonAcc = {
    TOTAL_COUNT_FN -> Json.toJson(totalCount) :: super.toJsonAcc
  }
}

trait StylesOpt extends SmJsonResp {
  def styles: String

  override def toJsonAcc: FieldsJsonAcc = {
    STYLES_FN -> JsString(styles)  ::  super.toJsonAcc
  }
}

/** Focused APIv2 контейнер ответа, для рендера в JSON. */
case class FocusedAdsResp2(override val ads: Seq[FocRenderResult],
                           override val totalCount: Int,
                           override val styles: String)
extends Action with FocusedAdsT with TotalCountT with StylesOpt {
  override def action = FOC_ANSWER_ACTION
}


// ------------------------------------------------------------------------------


/** Аддон для поддержки возвращаемого заголовка. */
trait TitleOpt extends SmJsonResp {
  def titleOpt: Option[String]
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _titleOpt = titleOpt
    if (_titleOpt.isDefined)
      TITLE_FN -> JsString(_titleOpt.get) :: acc0
    else
      acc0
  }
}

/** Аддон для флага оценки сервера достаточности геолокации. */
trait GeoAccurEnought extends SmJsonResp {
  def geoAccurEnought: Option[Boolean]
  override def toJsonAcc: FieldsJsonAcc = {
    val acc0 = super.toJsonAcc
    val _gaeOpt = geoAccurEnought
    if (_gaeOpt.isDefined)
      GEO_ACCURACY_ENOUGHT_FN -> JsBoolean(_gaeOpt.get)  ::  acc0
    else
      acc0
  }
}

trait CurrAdnId extends SmJsonResp {
  def currAdnId: Option[String]

  override def toJsonAcc: FieldsJsonAcc = {
    val caiJson = currAdnId.fold [JsValue] (JsNull) { cai => JsString(cai) }
    ADN_ID_FN -> caiJson  ::  super.toJsonAcc
  }
}

/**
 * showcase index ответ.
 * @param html верстка выдачи.
 * @param currAdnId Текущий id узла, к которому относится отображаемая выдача.
 */
case class ScIndexResp(
  html                            : JsString,
  override val currAdnId          : Option[String],
  override val geoAccurEnought    : Option[Boolean],
  override val titleOpt           : Option[String]
)
extends Action with HtmlOpt with CurrAdnId with GeoAccurEnought with TitleOpt {
  override def action = INDEX_RESP_ACTION
  override def htmlOpt: Option[JsString] = Some(html)
}


// ---------------------------------------------------------------------------------


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
