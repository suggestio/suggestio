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


  val AdvExtTgSuccess: EventType = new Val("a") {
    override def template = successTpl
  }

  val AdvExtTgInProcess: EventType = new Val("b") {
    override def template = targetInProcessTpl
  }
  
  val AdvExtTgError: EventType = new Val("c") {
    override def template = targetErrorTpl
  }

}
