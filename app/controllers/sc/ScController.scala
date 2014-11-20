package controllers.sc

import controllers.{routes, SioController}
import models.{AdCssArgs, Context, ScJsState, MAdT}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.twirl.api.Html
import util.cdn.CdnUtil
import util.jsa.JqAppend

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

  protected def jsAppendAdsCss(args: Seq[AdCssArgs])(implicit ctx: Context): JqAppend = {
    val call = routes.MarketShowcase.serveBlockCss(args)
    val call1 = CdnUtil.forCall(call)
    val html = s"""<link rel="stylesheet" type="text/css" href="${call1.url}" />"""
    JqAppend("head", JsString(html))
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


/** Для унифицированного сбора id рекламных карточек между несколькими logic'ами
  * испрользуется сий интерфейса. */
trait AdIdsFut {

  /** Вернуть id рекламных карточек, которые будут в итоге отправлены клиенту.
    * @return id карточек в неопределённом порядке. */
  def adIdsFut: Future[Iterable[String]]

  /** Вспомогательный метод для приведения списка карточек к списку id карточек. */
  // TODO Следует отвязать её от абстрактного Iterable через задействование CanBuildFrom[T].
  protected def madsFut2ids(madsFut: Future[Seq[MAdT]]): Future[Seq[String]] = {
    madsFut.map { mads =>
      mads.flatMap(_.id)
    }
  }
}
