package models.event

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import models.mctx.Context
import play.api.libs.json._
import play.twirl.api.{Html, Template2}
import views.html.lk.adv.ext.event._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 10:22
 * Description: Здесь лежит движок всех нотификаций, ориентированный на представление (рендер).
 * Смесь некоторой логики контроллера, утили и немного модели по типу BlocksConf.
 * Дергает разные шаблоны для рендера разных типов уведомлений, сгребая необходимые данные из других моделей.
 */
object MEventTypes extends StringEnum[MEventType] {

  /** Одна цель [[models.adv.MExtTarget]] успешно обработана. */
  case object AdvExtTgSuccess extends MEventType("a") {
    override def template = successTpl
  }

  /** Обработка одной цели [[models.adv.MExtTarget]] в процессе. */
  case object AdvExtTgInProcess extends MEventType("b") {
    override def template = targetInProcessTpl
  }

  /** Возникла ошибка при обработке одной цели [[models.adv.MExtTarget]]. */
  case object AdvExtTgError extends MEventType("c") {
    override def template = targetErrorTpl
  }

  /** Не удалась инициализация сервиса. Рендерим ошибку. */
  case object AdvServiceError extends MEventType("j") {
    override def template = serviceErrorTpl
  }

  /** Браузер блокирует всплывающие окна. */
  case object BrowserBlockPopupsError extends MEventType("k") {
    override def template = popupsBlockedTpl
  }


  override def values = findValues

}

sealed abstract class MEventType(override val value: String) extends StringEnumEntry {

  /** Шаблон для рендера одного события текущего типа. */
  def template: Template2[RenderArgs, Context, Html]

  /** Рендер одного события. */
  def render(args: RenderArgs)(implicit ctx: Context): Html = {
    template.render(args, ctx)
  }

}

object MEventType {

  implicit def mEventTypeFormat: Format[MEventType] =
    EnumeratumUtil.valueEnumEntryFormat( MEventTypes )

  @inline implicit def univEq: UnivEq[MEventType] = UnivEq.derive
  
}

