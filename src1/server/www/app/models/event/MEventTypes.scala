package models.event

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsT
import models.mctx.Context
import play.api.libs.json._
import play.twirl.api.{Html, Template2}
import views.html.lk.adv.ext.event._
import views.html.lk.event._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 10:22
 * Description: Здесь лежит движок всех нотификаций, ориентированный на представление (рендер).
 * Смесь некоторой логики контроллера, утили и немного модели по типу BlocksConf.
 * Дергает разные шаблоны для рендера разных типов уведомлений, сгребая необходимые данные из других моделей.
 */
object MEventTypes extends Enumeration with EnumMaybeWithName with EnumJsonReadsT {

  protected abstract class Val(val strId: String) extends super.Val(strId) {

    /** Шаблон для рендера одного события текущего типа. */
    def template: Template2[RenderArgs, Context, Html]

    /** Рендер одного события. */
    def render(args: RenderArgs)(implicit ctx: Context): Html = {
      template.render(args, ctx)
    }
  }

  override type T = Val


  /** Одна цель [[models.adv.MExtTarget]] успешно обработана. */
  val AdvExtTgSuccess: MEventType = new Val("a") {
    override def template = successTpl
  }

  /** Обработка одной цели [[models.adv.MExtTarget]] в процессе. */
  val AdvExtTgInProcess: MEventType = new Val("b") {
    override def template = targetInProcessTpl
  }

  /** Возникла ошибка при обработке одной цели [[models.adv.MExtTarget]]. */
  val AdvExtTgError: MEventType = new Val("c") {
    override def template = targetErrorTpl
  }

  /** Когда узел создан, нужно добавить владельцу указание на возможность создать ещё один узел. */
  val YouCanCreateNewShop: MEventType = new Val("e") {
    override def template = _youCanUseAddShopBtnEvtTpl
  }

  /** После создания узла, уведомление о возможности попользоваться удобным менеджером рекламных карточек. */
  val StartYourWorkUsingCardMgr: MEventType = new Val("f") {
    /** Шаблон для рендера одного события текущего типа. */
    override def template = _startYourWorkUsingCardMgrEvtTpl
  }

  /** Появился входящий запрос по размещению. */
  val AdvReqIncoming: MEventType = new Val("g") {
    override def template = _advRequestedEvtTpl
  }

  /** Исходящее размещение заапрувлено. */
  val AdvOutcomingOk: MEventType = new Val("h") {
    override def template = _yourAdvApprovedEvtTpl
  }

  /** Исходящее размещение отклонено. */
  val AdvOutcomingRefused: MEventType = new Val("i") {
    override def template = _yourAdvRefusedEvtTpl
  }

  /** Не удалась инициализация сервиса. Рендерим ошибку. */
  val AdvServiceError: MEventType = new Val("j") {
    override def template = serviceErrorTpl
  }

  /** Браузер блокирует всплывающие окна. */
  val BrowserBlockPopupsError: MEventType = new Val("k") {
    override def template = popupsBlockedTpl
  }


  /** Десериализация из JSON. */
  override implicit val reads: Reads[T] = {
    super.reads
  }

}
