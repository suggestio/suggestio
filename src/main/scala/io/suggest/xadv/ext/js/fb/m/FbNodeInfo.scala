package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.FromJsonT

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 17:17
 * Description: Параметры вызова и результаты инфы по узлу.
 * Эта модель абстрактно затрагивает сразу несколько API.
 * Args-модель сериализуется в path + query_string.
 * @see [[https://developers.facebook.com/docs/graph-api/reference/v2.2/page#read]]
 * @see [[https://developers.facebook.com/docs/graph-api/reference/v2.2/user#read]]
 * @see [[https://developers.facebook.com/docs/graph-api/reference/v2.2/event#read]]
 * @see [[https://developers.facebook.com/docs/graph-api/reference/v2.2/group#read]]
 */

object FbNodeInfoArgs {

  def FIELDS_ONLY_ID = Seq("id")
  def FIELDS_ALL     = Seq.empty

}

/**
 * Экземпляр распарсенной инфы по узлу.
 * @param id запрашиваемого узла.
 * @param fields Запрашиваемые поля. Безопасно вызывать [] (все поля) и [id] (только id).
 *               По умолчанию -- все поля.
 * @param metadata Запрашивать метаданные?
 * @param accessToken Запросить access token?
 */
case class FbNodeInfoArgs(
  id          : String,
  fields      : Seq[String] = FbNodeInfoArgs.FIELDS_ALL,
  accessToken : Boolean     = false,
  metadata    : Boolean     = false
) {

  /** Сериализация в path. */
  def toPath: String = {
    // Собираем qs-часть.
    var qsAcc: List[String] = Nil
    if (fields.nonEmpty)
      qsAcc ::= "fields=" + fields.mkString(",")
    if (metadata)
      qsAcc ::= "metadata=1"
    if (accessToken)
      qsAcc ::= "access_token=1"
    // Собираем итоговую строку.
    var acc = "/" + id
    if (qsAcc.nonEmpty)
      acc = acc + "?" + qsAcc.mkString("&")
    acc
  }

}


/** Статическая часть модели результата вызова getNodeInfo(). */
object FbNodeInfoResult extends FromJsonT {
  override type T = FbNodeInfoResult

  /** Десериализация из JSON. */
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]]: WrappedDictionary[Any]
    FbNodeInfoResult(
      nodeId      = d.get("id").map(_.toString),
      accessToken = d.get("access_token").map(_.toString),
      error       = d.get("error").map(FbApiError.fromJson),
      metadata    = d.get("metadata").map(FbNodeMetaData.fromJson)
    )
  }

}


/** Интерфейс опционального access token. Используется в разных местах. */
trait IFbAccessTokenOpt {
  def accessToken : Option[String]
}


/**
 * Распарсенный результат ответа.
 * @param nodeId id узла, если есть.
 * @param accessToken Access Token, если есть.
 * @param error Данные по ошибке, если произошла.
 */
case class FbNodeInfoResult(
  nodeId      : Option[String],
  accessToken : Option[String],
  error       : Option[FbApiError],
  metadata    : Option[FbNodeMetaData]
) extends IFbAccessTokenOpt
