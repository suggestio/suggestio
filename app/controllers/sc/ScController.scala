package controllers.sc

import controllers.{routes, SioController}
import models._
import models.msc.ScJsState
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsString
import play.twirl.api.{Txt, Html}
import util.cdn.CdnUtil
import util.jsa.JsAction
import views.txt.blocks.common._

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки MarketShowcase-контроллера.
 */
trait ScController extends SioController {

  /**
   * Вспомогательный метод для генерации ссылки на css блоков из списка данных об этих блоках.
   * Она работает с данными от разных logic'ов, наследующих [[AdCssRenderArgs]].
   * @param args Данные по карточкам и их рендеру.
   * @param ctx Контекст.
   * @return Отрендеренный Html: link rel css.
   */
  protected def htmlAdsCssLink(args: Seq[AdCssArgs])(implicit ctx: Context): Html = {
    val call = routes.MarketShowcase.serveBlockCss(args)
    val call1 = CdnUtil.forCall(call)
    views.html.sc.stuff._cssLinkTpl(call1)
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
    def adsCssFieldRenderArgsFut: Future[immutable.Seq[blk.FieldCssRenderArgsT]]
    
    def adsFieldCssRenderFut: Future[immutable.Seq[Txt]] = {
      adsCssFieldRenderArgsFut flatMap { args =>
        // TODO Нужно обрать из выхлопа лишнии пробелы и пустые строки. Это сократит выхлоп до 10%.
        Future.traverse(args) { cssRenderArgs =>
          Future {
            _textCss(cssRenderArgs)
          }
        }
      }
    }

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    def adsCssRenderArgsFut: Future[immutable.Seq[blk.IRenderArgs]]

    /** Рендер обрамляющего css блоков на основе соотв. параметров. */
    def adsCssRenderFut: Future[immutable.Seq[Txt]] = {
      adsCssRenderArgsFut flatMap { args =>
        Future.traverse(args) { cra =>
          Future {
            _blockCss(cra)
          }
        }
      }
    }

    /** Вспомогательная функция для подготовки данных к рендеру css'ок: приведение рекламной карточки к css-параметрам. */
    protected def mad2craIter(brArgs: blk.IRenderArgs, cssClasses: Seq[String]): Iterator[blk.FieldCssRenderArgsT] = {
      import brArgs.mad
      val bc = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
      mad.offers.iterator.flatMap { offer =>
        val t1 = offer.text1.map { text1 => ("title", text1, bc.titleBf, 0) }
        val fields = t1.toSeq
        fields.iterator.map { case (fid, aosf, bf, yoff) =>
          blk.FieldCssRenderArgs2(
            aovf    = aosf,
            bf      = bf,
            brArgs  = brArgs,
            offerN  = offer.n,
            yoff    = yoff,
            fid     = fid,
            cssClasses = cssClasses,
            isFocused  = brArgs.isFocused
          )
        }
      }
    }

    /** Генерация js-экшена для рендера стилей. */
    def jsAppendCssAction(html: JsString): JsAction

    /** Отрендерить стили в Txt для всех необходимых блоков. */
    def jsAdsCssFut: Future[Txt] = {
      val _adsCssRenderFut = adsCssRenderFut
      for {
        fieldRenders  <- adsFieldCssRenderFut
        adsCssRenders <- _adsCssRenderFut
      } yield {
        val blkCssTxts = new Txt(adsCssRenders)
        val fieldCssTxts = new Txt(fieldRenders)
        new Txt( List(blkCssTxts, fieldCssTxts) )
      }
    }

    /** Отрендерить js для добавления стилей блоков в документ выдачи. */
    def jsAppendAdsCssFut: Future[JsAction] = {
      jsAdsCssFut map { jsAdsCss =>
        val html = List(Txt("<style>"), jsAdsCss, Txt("</style>"))
        val data = JsString( new Txt(html) )
        jsAppendCssAction(data)
      }
    }

  }

}

