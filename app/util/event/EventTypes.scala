package util.event

import java.{util => ju}

import io.suggest.model.EnumMaybeWithName
import models.Context
import models.event._
import play.twirl.api.Html

import scala.concurrent.Future

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

    /** Рендер события. */
    def render(args: IArgsInfo, runtimeArgs: Map[ArgName, Any])(implicit ctx: Context): Future[Html]

    /**
     * Десериализация инфы по аргументам из JSON.
     * @param raw Jackson json map (js object) или null/undefined, но гипотетически может быть и нечто ещё.
     * @return Конкретная реализация
     */
    def deserializeArgsInfo(raw: Any): IArgsInfo
  }

  type EventType = Val
  override type T = EventType



  // TODO Разные типы нотификаций тут.
}
