package util.notify

import io.suggest.model.EnumMaybeWithName
import models.Context
import models.notify._
import play.twirl.api.Html
import java.{util => ju}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 10:22
 * Description: Здесь лежит движок всех нотификаций, ориентированный на представление (рендер).
 * Смесь некоторой логики контроллера, утили и немного модели по типу [[util.blocks.BlocksConf]].
 * Дергает разные шаблоны для рендера разных типов уведомлений, сгребая необходимые данные из других моделей.
 */
object NotifyTypes extends Enumeration with EnumMaybeWithName {

  protected abstract class Val(val strId: String) extends super.Val(strId) {

    //def render(rawArgs: ju.Map[_,_])(implicit ctx: Context): Future[Html]

    /**
     * Десериализация инфы по аргументам из JSON.
     * @param raw Jackson json map (js object) или null/undefined, но гипотетически может быть и нечто ещё.
     * @return Конкретная реализация
     */
    def deserializeArgsInfo(raw: Any): IArgsInfo
  }

  type NotifyType = Val
  override type T = NotifyType



  // TODO Разные
}
