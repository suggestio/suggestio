package controllers.sc

import controllers.routes
import models.mctx.Context
import models.msc.AdCssArgs
import play.twirl.api.Html
import util.n2u.IN2NodesUtilDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 14:53
  * Description: Утиль для рендера CSS свалена здесь.
  */
trait ScCssUtil
  extends ScController
  with IN2NodesUtilDi
{

  /**
   * Вспомогательный метод для генерации ссылки на css блоков из списка данных об этих блоках.
   * Она работает с данными от разных logic'ов, наследующих [[AdCssRenderArgs]].
   * @param args Данные по карточкам и их рендеру.
   * @param ctx Контекст.
   * @return Отрендеренный Html: link rel css.
   */
  protected def htmlAdsCssLink(args: Seq[AdCssArgs])(implicit ctx: Context): Html = {
    val call = routes.Sc.serveBlockCss(args)
    val call1 = cdnUtil.forCall(call)
    views.html.sc.stuff._cssLinkTpl(call1)
  }

  /** Для унифицированного сбора данных для рендера css блоков, тут интерфейс. */
  trait AdCssRenderArgs {

    /** Вернуть данные по рендеру css для случая внешнего рендера, т.е. когда клиент получает
      * css отдельным запросом.
      * @return последовательность аргументов для генерации ссылки на css. */
    def adsCssExternalFut: Future[Seq[AdCssArgs]]

  }

}
