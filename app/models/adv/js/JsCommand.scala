package models.adv.js

import io.suggest.adv.ext.model.MCommandTypesT
import io.suggest.adv.ext.model.JsCommand._
import io.suggest.adv.ext.model.ctx.MAskActionsT
import io.suggest.model.EnumJsonReadsT
import io.suggest.model.EsModel.FieldsJsonAcc
import models.adv.js.ctx.MJsCtx
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 12:34
 * Description: Язык общения акторов, занимается описанием взаимодействия между акторами разных уровней:
 * На этом языке разговаривают ws-актор-супервизор с подчинёнными target-level-акторами.
 */

object IWsCmd {

  /** json writer. */
  implicit def writes = new Writes[IWsCmd] {
    override def writes(o: IWsCmd) = o.toJson
  }

}

/** Абстрактная команда, целиком отправляемая на клиент js-подсистеме по ws. */
sealed trait IWsCmd {
  /** Тип команды: js или нечто в json action формате. */
  def ctype   : MJsCmdType

  /** Режим отправки: асинхронный или в порядке очереди. */
  def sendMode: CmdSendMode = CmdSendModes.Async

  /** Сериализовать в JSON. */
  def toJson: JsObject = JsObject(toJsonAcc)
  def toJsonAcc: FieldsJsonAcc = {
    List(TYPE_FN -> Json.toJson(ctype))
  }
}


/** Абстрактная команда, состоящая из js-кода. */
trait IJsCmd extends IWsCmd {
  override def ctype = MJsCmdTypes.JavaScript

  /** JavaScript код на исполнение. */
  def jsCode: String

  /** Если этот код открывает popup window, то тут должно быть true.
    * js отправит попап в очередь попапов. */
  def isPopup: Boolean

  override def toJsonAcc: FieldsJsonAcc = {
    JS_CODE_FN -> Json.toJson(jsCode) ::
    IS_POPUP_FN -> JsBoolean(isPopup) ::
    super.toJsonAcc
  }
}

/** Дефолтовая реализация [[IJsCmd]]. */
case class JsCmd(jsCode: String, isPopup: Boolean = false) extends IJsCmd


/** Команда к js-подсистеме внешнего размещения в json-формате. */
trait IJsonActionCmd extends IWsCmd {
  override def ctype = MJsCmdTypes.Action

  /** Ключ адресата для составления ответа. */
  def replyTo : Option[String]

  /** Контекст запроса. */
  def mctx: MJsCtx

  override def toJsonAcc: FieldsJsonAcc = {
    var acc = super.toJsonAcc
    acc ::= MCTX_FN -> Json.toJson(mctx)
    val _replyTo = replyTo
    if (_replyTo.isDefined)
      acc ::= REPLY_TO_FN -> Json.toJson(_replyTo.get)
    acc
  }
}


/** Закидывание в контекст данных по текущему экшену. */
trait IJsonActionCtxPatcher extends IJsonActionCmd {
  /** Исходный контекст. */
  def mctx0: MJsCtx
  /** Текущий экшен. */
  def action: MJsAction
  override def mctx: MJsCtx = {
    val _act = action
    if (!(mctx0.action contains _act)) {
      mctx0.copy(
        action = Some(_act)
      )
    } else {
      mctx0
    }
  }
}


/** Команда для выставления значения в хранилище браузера. */
case class StorageSetCmd(mctx: MJsCtx) extends IJsonActionCmd {
  override def replyTo = None
}
/** Команда для чтения значения из хранилища браузера. */
case class StorageGetCmd(mctx: MJsCtx, replyTo: Option[String]) extends IJsonActionCmd


/** Допустимые режимы отправки js-кода в ws. */
object CmdSendModes extends Enumeration {
  type T = Value
  val Async, Queued = Value : T
}


/** Реализация модели типов команд, отправляемых клиенту по ws. */
object MJsCmdTypes extends MCommandTypesT

/** Реализация модели json-экшенов. */
object MJsActions extends MAskActionsT with EnumJsonReadsT

