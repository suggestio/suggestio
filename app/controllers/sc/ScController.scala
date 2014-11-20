package controllers.sc

import controllers.SioController
import io.suggest.model.EsModel.FieldsJsonAcc
import models.ScJsState
import play.api.libs.Jsonp
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.twirl.api.Html

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки MarketShowcase-контроллера.
 */
trait ScController extends SioController {

  /** Параллельный маппинг scala-коллекции. Отмаппленных в результатах явно восстанавливается исходный порядок.
    * @param vs Исходная коллекция.
    * @param r функция-маппер.
    * @return Фьючерс с отмапленной коллекцией в исходном порядке.
    */
  protected def parTraverseOrdered[T, V](vs: Seq[V], startIndex: Int = 0)(r: (V, Int) => Future[T]): Future[Seq[T]] = {
    val vs1 = vs.zipWithIndex
    Future.traverse(vs1) { case (mad, index) =>
      r(mad, startIndex + index)
        .map { index -> _ }
    } map {
      _.sortBy(_._1)
        .map(_._2)
    }
  }


  /** Некоторые асинхронные шаблоны выдачи при синхронном рендере требуют для себя js-состояние. */
  trait JsStateRenderWrapper {
    /**
     * Запустить синхронный рендер шаблона используя указанное js-состояние выдачи.
     * @param jsStateOpt None - происходит асинхронный рендер. Some() - идёт синхронный рендер с указанным состоянием.
     * @return Отрендеренный HTML.
     */
    def apply(jsStateOpt: Option[ScJsState] = None): Html
  }

}
