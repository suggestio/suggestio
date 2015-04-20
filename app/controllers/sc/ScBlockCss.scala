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

import scala.concurrent.Future

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
    val resFut = madsFut.flatMap { mads =>
      Future.traverse(mads) { mad =>
        val arg = argsMap(mad.id.get)
        val bc = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
        // Картинка вроде нужна, но стоит в этом убедиться... Future для распаралеливания и на случай если картинка понадобиться
        Future {
          val brArgs = blk.RenderArgs(
            mad           = mad,
            szMult        = arg.szMult,
            inlineStyles  = false,
            bgImg         = None
          )
          val offerFieldsTxts = mad.offers
            .iterator
            .flatMap { offer =>
              offer.text1.map { t1 => blk.FieldCssRenderArgs2(brArgs, t1, bc.titleBf, offer.n, yoff = 0, fid = "title") }
            }
            .map { cssRenderArgs =>
              _textCss(cssRenderArgs): Txt
            }
            .toList
          val preableCssTxt = _blockCss(brArgs): Txt
          preableCssTxt :: offerFieldsTxts
        }
      } map { txts1 =>
        val txts2 = txts1.iterator.flatMap(identity).toStream
        new Txt(txts2)
      }
    }
    resFut map {
      Ok(_).as("text/css")
    }
  }


}
