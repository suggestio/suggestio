package controllers.sc

import controllers.SioController
import io.suggest.ym.model.MAd
import io.suggest.ym.model.ad.Coords2D
import models.blk
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Action
import play.twirl.api.Txt
import util.PlayMacroLogsI
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.blocks.BlocksConf
import util.blocks.BlocksConf.BlockConf
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
   * @param adIdsRaw Список id карточек, для которых надо вернуть css.
   * @return 200 Ok с отрендеренным css в неопределённом порядке.
   */
  def serveBlockCss(szMult: Float, adIdsRaw: String) = Action.async { implicit request =>
    val adIds = adIdsRaw.split("[/,]+")
    // TODO Надо переписать это дело через асинхронные enumerator'ы
    val resFut = MAd.multiGet(adIds).map { mads =>
      val txts = mads.iterator.flatMap { mad =>
        val bc = BlocksConf.applyOrDefault(mad.blockMeta.blockId)
        mad.offers
          .iterator
          .flatMap { offer =>
            val t1r = offer.text1.map { t1 => blk.CssRenderArgs2(mad.id, t1, bc.titleBf, offer.n, yoff = 0,  szMult, "title") }
            val t2r = offer.text2.map { t2 => blk.CssRenderArgs2(mad.id, t2, bc.descrBf, offer.n, yoff = 25, szMult, "descr") }
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
