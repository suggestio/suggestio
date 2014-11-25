package controllers.sc

import controllers.SioController
import io.suggest.ym.model.MAd
import models.{AdCssArgs, blk}
import play.api.mvc.Action
import play.twirl.api.Txt
import util.PlayMacroLogsI
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.blocks.BlocksConf
import views.txt.blocks.common._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.14 13:26
 * Description: Экшен и сопутствуюшая утиль для контроллера, добавляющая экшены сборки и раздачи
 * динамических css'ок для блоков выдачи.
 * Изначально, весь css рендерился прямо в выдачу, что вызывало ряд проблем.
 */
trait ScBlockCss extends SioController with PlayMacroLogsI {

  /**
   * Экшен раздачи css'ок.
   * @param args Список id карточек, для которых надо вернуть css и параметры их рендера.
   * @return 200 Ok с отрендеренным css в неопределённом порядке.
   */
  def serveBlockCss(args: Seq[AdCssArgs]) = Action.async { implicit request =>
    // TODO Надо переписать это дело через асинхронные enumerator'ы
    val madsFut = MAd.multiGet( args.iterator.map(_.adId) )
    val argsMap = args.iterator
      .map(arg => arg.adId -> arg)
      .toMap
    val resFut = madsFut.map { mads =>
      val txts = mads.iterator.flatMap { mad =>
        val arg = argsMap(mad.id.get)
        val bc = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
        mad.offers
          .iterator
          .flatMap { offer =>
            val t1r = offer.text1.map { t1 => blk.CssRenderArgs2(mad, t1, bc.titleBf, offer.n, yoff = 0,  arg.szMult, "title") }
            val t2r = offer.text2.map { t2 => blk.CssRenderArgs2(mad, t2, bc.descrBf, offer.n, yoff = 25, arg.szMult, "descr") }
            t1r ++ t2r
          }
          .map { cssRenderArgs =>
            _blockStyleCss(cssRenderArgs) : Txt
          }
      }
      new Txt(txts.toStream)  // toSeq почему-то не прокатывает
    }
    resFut map {
      Ok(_).as("text/css")
    }
  }


}
