package controllers.sc

import controllers.SioController
import io.suggest.model.n2.node.IMNodes
import io.suggest.util.logs.IMacroLogs
import models.msc.AdCssArgs
import models.blk
import play.twirl.api.Txt
import util.acl.{IIgnoreAuth, IgnoreAuth}
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
  with IMacroLogs
  with IN2NodesUtilDi
  with IMNodes
  with IIgnoreAuth
{

  import mCommonDi._

  /**
   * Экшен раздачи css'ок.
   * @param args Список id карточек, для которых надо вернуть css и параметры их рендера.
   * @return 200 Ok с отрендеренным css в неопределённом порядке.
   */
  def serveBlockCss(args: Seq[AdCssArgs]) = ignoreAuth().async { implicit request =>
    // TODO Надо переписать это дело через асинхронные enumerator'ы
    val madsFut = mNodes.multiGetRev( args.iterator.map(_.adId) )
    val argsMap = args.iterator
      .map(arg => arg.adId -> arg)
      .toMap
    implicit val ctx = getContext2
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
            .map { entity =>
              val cssRenderArgs = blk.FieldCssRenderArgs(brArgs, bc.titleBf, entity, yoff = 0 )
              _textCss(cssRenderArgs): Txt
            }
            .toList
          val preableCssTxt = _blockCss(brArgs)(ctx): Txt
          preableCssTxt :: offerFieldsTxts
        }
      } map { txts1 =>
        val txts2 = txts1.iterator
          .flatten
          .toStream
        new Txt(txts2)
      }
    }
    resFut map {
      Ok(_)
        .as( CSS )
    }
  }


}
