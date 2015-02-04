package util.event

import io.suggest.model.EnumMaybeWithName
import models.Context
import models.event._
import play.twirl.api.{Template2, Html}

import views.html.lk.event._
import views.html.lk.adv.ext.event._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 10:22
 * Description: Здесь лежит движок всех нотификаций, ориентированный на представление (рендер).
 * Смесь некоторой логики контроллера, утили и немного модели по типу [[util.blocks.BlocksConf]].
 * Дергает разные шаблоны для рендера разных типов уведомлений, сгребая необходимые данные из других моделей.
 */
object EventTypes extends Enumeration with EnumMaybeWithName {

  protected abstract class Val(val strId: String) extends super.Val(strId) {

    /** Шаблон для рендера одного события текущего типа. */
    def template: Template2[RenderArgs, Context, Html]

    /** Рендер одного события. */
    def render(args: RenderArgs)(implicit ctx: Context): Html = {
      template.render(args, ctx)
    }
  }

  type EventType = Val
  override type T = EventType


  /** Одна цель [[models.adv.MExtTarget]] успешно обработана. */
  val AdvExtTgSuccess: EventType = new Val("a") {
    override def template = successTpl
  }

  /** Обработка одной цели [[models.adv.MExtTarget]] в процессе. */
  val AdvExtTgInProcess: EventType = new Val("b") {
    override def template = targetInProcessTpl
  }

  /** Возникла ошибка при обработке одной цели [[models.adv.MExtTarget]]. */
  val AdvExtTgError: EventType = new Val("c") {
    override def template = targetErrorTpl
  }

  /** Когда узел [[models.MAdnNode]] создан, нужно выводить приглашение для включения в геосеть s.io. */
  val NodeGeoWelcome: EventType = new Val("d") {
    override def template = _nodeGeoWelcomeEvtTpl
  }

  /** Когда узел создан, нужно добавить владельцу указание на возможность создать ещё один узел. */
  val YouCanCreateNewShop: EventType = new Val("e") {
    override def template = _youCanUseAddShopBtnEvtTpl
  }

  /** После создания узла, уведомление о возможности попользоваться удобным менеджером рекламных карточек. */
  val StartYourWorkUsingCardMgr: EventType = new Val("f") {
    /** Шаблон для рендера одного события текущего типа. */
    override def template = _startYourWorkUsingCardMgrEvtTpl
  }

  /** Появился входящий запрос по размещению. */
  val AdvReqIncoming: EventType = new Val("g") {
    override def template = _advRequestedEvtTpl
  }

  /** Исходящее размещение заапрувлено. */
  val AdvOutcomingOk: EventType = new Val("h") {
    override def template = _yourAdvApprovedEvtTpl
  }

  /** Исходящее размещение отклонено. */
  val AdvOutcomingRefused: EventType = new Val("i") {
    override def template = _yourAdvRefusedEvtTpl
  }

  /** Не удалась инициализация сервиса. Рендерим ошибку. */
  val AdvServiceError: EventType = new Val("j") {
    override def template = serviceErrorTpl
  }

}
