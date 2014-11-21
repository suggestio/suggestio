package controllers.sc

import controllers.{routes, SioController}
import models._
import models.blk.{SzMult_t, CssRenderArgsT}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.twirl.api.{Txt, Html}
import util.cdn.CdnUtil
import util.jsa.JsAppendByTagName

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

  protected def htmlAdsCssLink(args: Seq[AdCssArgs])(implicit ctx: Context): Html = {
    val call = routes.MarketShowcase.serveBlockCss(args)
    val call1 = CdnUtil.forCall(call)
    views.html.market.showcase.stuff._cssLinkTpl(call1)
  }
  protected def jsAppendAdsCss(args: Seq[CssRenderArgsT])(implicit ctx: Context): Future[JsAppendByTagName] = {
    // TODO Нужно обрать из выхлопа лишнии пробелы и пустые строки. Это сократит выхлоп до 10%.
    Future.traverse(args) { cssRenderArgs =>
      Future {
        views.txt.blocks.common._blockStyleCss(cssRenderArgs)
      }
    } map { renders =>
      val styleTxt = new Txt(renders.toList)
      val html = List(new Txt("<style>"), styleTxt, new Txt("</style>"))
      JsAppendByTagName("head", JsString(new Txt(html)))
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


/** Для унифицированного сбора данных для рендера css блоков, тут интерфейс. */
trait AdCssRenderArgs {

  /** Вернуть данные по рендеру css для случая внешнего рендера, т.е. когда клиент получает
    * css отдельным запросом.
    * @return последовательность аргументов для генерации ссылки на css. */
  def adsCssExternalFut: Future[Seq[AdCssArgs]]

  /**
   * Вернуть данные по пакетному рендеру css блоков одновременно с текущим запросом.
   * @return последовательность аргументов для вызова рендера как можно скорее.
   */
  def adsCssInternalFut: Future[Seq[CssRenderArgsT]]

  /** Вспомогательная функция для подготовки данных к рендеру css'ок: приведение рекламной карточки к css-параметрам. */
  protected def mad2craIter(mad: MAd, szMult: SzMult_t): Iterator[CssRenderArgsT] = {
    val bc = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
    mad.offers.iterator.flatMap { offer =>
      val t1 = offer.text1.map { text1 => ("title", text1, bc.titleBf, 0) }
      val t2 = offer.text2.map { text2 => ("descr", text2, bc.descrBf, 25) }
      val fields = t1 ++ t2
      fields.iterator.map { case (fid, aosf, bf, yoff) =>
        blk.CssRenderArgs2(
          madId   = mad.id,
          aovf    = aosf,
          bf      = bf,
          szMult  = szMult,
          offerN  = offer.n,
          yoff    = yoff,
          fid     = fid
        )
      }
    }
  }

}

case class AdAndBrArgs(mad: MAd, brArgs: blk.RenderArgs)
