package util.event

import util.DkeyContainerT
import play.api.libs.json._
import io.suggest.util.event.subscriber.SioEventTJSable
import io.suggest.event.SioNotifier.Classifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 11:36
 * Description: События Qi (Quick Install). Здесь группа родственных событий, используемых для взаимодействия между
 * юзером и системой установки доменов.
 */

object QiEvent {
  val headSneToken = Some("qi")

  def getClassifier(dkeyOpt:Option[String] = None,  qiIdOpt:Option[String] = None,  isSuccessOpt: Option[Boolean] = None): Classifier = {
    List(headSneToken, dkeyOpt, qiIdOpt, isSuccessOpt)
  }
}


// Базовый трейт для QiEvent'ов, которые передаются юзерам.
trait QiEvent extends SioEventTJSable with DkeyContainerT {
  import play.api.libs.json._

  val dkey: String
  val url: String
  val qiIdOpt: Option[String]
  def isSuccess : Boolean


  def getClassifier: Classifier = {
    QiEvent.getClassifier(dkeyOpt = Some(dkey), qiIdOpt=qiIdOpt, isSuccessOpt = Some(isSuccess))
  }

  def toJson: JsValue = {
    val jsonFields = {
      "type" -> JsString(jsonEventType) ::
      "url"  -> JsString(url) ::
      jsonMapTail ++ dkeyJsProps
    }
    JsObject(jsonFields)
  }

  def jsonEventType : String
  def jsonMapTail : List[(String, JsValue)]
}


/**
 * Уведомление об ошибке qi.
 * @param dkey Ключ домена
 * @param url Ссылка, которая проверялась на предмет qi
 * @param msg Сообщение о проблеме.
 */
case class QiError(dkey:String, qiIdOpt:Option[String], url:String, msg:String) extends QiEvent {
  val isSuccess = false

  def jsonEventType: String = "qi.error"
  def jsonMapTail: List[(String, JsValue)] = List("error" -> JsString(msg))
}


/**
 * Уведомление об успешном прохождении qi.
 * @param dkey Ключ домена
 * @param url Ссылка, для информации.
 */
case class QiSuccess(dkey:String, qi_id:String, url:String) extends QiEvent {
  val isSuccess = true
  val qiIdOpt = Some(qi_id)

  def jsonEventType: String = "qi.success"
  def jsonMapTail: List[(String, JsValue)] = List("is_js_installed" -> JsBoolean(isSuccess))
}

