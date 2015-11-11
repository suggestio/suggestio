package controllers.sc

import controllers.routes
import models.{blk, Context}
import models.msc.AdCssArgs
import play.api.libs.json.JsString
import play.twirl.api.{Txt, Html}
import util.blocks.BlocksConf
import util.jsa.JsAction
import views.txt.blocks.common.{_blockCss, _textCss}

import scala.collection.immutable
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.15 14:53
  * Description: Утиль для рендера CSS свалена здесь.
  */
trait ScCssUtil extends ScController {

  /**
   * Вспомогательный метод для генерации ссылки на css блоков из списка данных об этих блоках.
   * Она работает с данными от разных logic'ов, наследующих [[AdCssRenderArgs]].
   * @param args Данные по карточкам и их рендеру.
   * @param ctx Контекст.
   * @return Отрендеренный Html: link rel css.
   */
  protected def htmlAdsCssLink(args: Seq[AdCssArgs])(implicit ctx: Context): Html = {
    val call = routes.MarketShowcase.serveBlockCss(args)
    val call1 = cdnUtil.forCall(call)
    views.html.sc.stuff._cssLinkTpl(call1)
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
      val bc = mad.ad.blockMeta
        .fold(BlocksConf.DEFAULT)(bm => BlocksConf(bm.blockId))
      mad.ad.entities
        .valuesIterator
        .flatMap { ent =>
          val t1 = ent.text.map { text1 => ("title", text1, bc.titleBf, 0) }
          val fields = t1.toSeq
          fields.iterator.map { case (fid, aosf, bf, yoff) =>
            blk.FieldCssRenderArgs2(
              aovf        = aosf,
              bf          = bf,
              brArgs      = brArgs,
              offerN      = ent.id,
              yoff        = yoff,
              fid         = fid,
              cssClasses  = cssClasses,
              isFocused   = brArgs.isFocused
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
