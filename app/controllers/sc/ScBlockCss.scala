package controllers.sc

import controllers.SioController
import io.suggest.model.n2.node.IMNodes
import models.msc.AdCssArgs
import models.blk
import play.api.mvc.Action
import play.twirl.api.Txt
import util.PlayMacroLogsI
import util.n2u.IN2NodesUtilDi
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
trait ScBlockCss
  extends SioController
  with PlayMacroLogsI
  with IN2NodesUtilDi
  with IMNodes
{

  import mCommonDi._

  /**
   * Экшен раздачи css'ок.
   * @param args Список id карточек, для которых надо вернуть css и параметры их рендера.
   * @return 200 Ok с отрендеренным css в неопределённом порядке.
   */
  def serveBlockCss(args: Seq[AdCssArgs]) = Action.async { implicit request =>
    // TODO Надо переписать это дело через асинхронные enumerator'ы
    val madsFut = mNodes.multiGetRev( args.iterator.map(_.adId) )
    val argsMap = args.iterator
      .map(arg => arg.adId -> arg)
      .toMap
    val resFut = madsFut.flatMap { mads =>
      Future.traverse(mads) { mad =>
        val arg = argsMap(mad.id.get)
        val bc = n2NodesUtil.bc(mad)
        // Картинка вроде нужна, но стоит в этом убедиться... Future для распаралеливания и на случай если картинка понадобиться
        Future {
          val brArgs = blk.RenderArgs(
            mad           = mad,
            bc            = bc,
            szMult        = arg.szMult,
            inlineStyles  = false,
            bgImg         = None
          )
          val offerFieldsTxts = mad.ad.entities
            .valuesIterator
            .flatMap { offer =>
              offer.text.map { t1 =>
                blk.FieldCssRenderArgs2(brArgs, t1, bc.titleBf, offer.id, yoff = 0, fid = "title")
              }
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
