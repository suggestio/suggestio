package controllers

import io.suggest.es.model.EsModelDi
import io.suggest.img.{MImgFmtJvm, MImgFormats}
import io.suggest.n2.node.IMNodes
import io.suggest.util.logs.IMacroLogs
import models.blk.{OneAdQsArgs, OneAdWideQsArgs}
import models.msc.{OneAdRenderVariant, OneAdRenderVariants}
import models.msys.MShowOneAdFormTplArgs
import models.req.IAdReq
import play.api.data.{Form, Mapping}
import play.api.mvc.{Call, Result}
import util.acl.IIsSuMad
import util.n2u.IN2NodesUtilDi
import util.FormUtil
import util.adv.IAdvUtilDi
import views.html.sys1.market.ad.one._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 15:50
 * Description: sys-раздел для отладки рендера карточек в картинки или html.
 */
class SysAdRenderUtil {

  import play.api.data.Forms._

  /** Внутренний маппинг контроллера для OneAdQsArgs. */
  def oneAdQsArgsM(madId: String): Mapping[OneAdQsArgs] = {
    mapping(
      "szMult" -> FormUtil.szMultM,
      "vsn"    -> FormUtil.esVsnOptM,
      "imgFmt" -> MImgFmtJvm.mapping,
      "wide"   -> OneAdWideQsArgs.optMapper
    )
    { OneAdQsArgs(madId, _, _, _, _) }
    { OneAdQsArgs.unapply(_)
      .map { case (_, a, b, c, d) => (a, b, c, d) }
    }
  }

  /** Маппинг для формы OneAdQsArgs. */
  def oneAdQsArgsFormM(madId: String): Form[OneAdQsArgs] = {
    Form( oneAdQsArgsM(madId) )
  }

}


// TODO Замёржить в SysMarket
/** Аддон для sys-контроллера для добавления экшенов, связанных с рендером карточки. */
trait SysAdRender
  extends ISioControllerApi
  with IMacroLogs
  with IIsSuMad
  with IN2NodesUtilDi
  with IAdvUtilDi
  with EsModelDi
  with IMNodes
{

  import sioControllerApi._
  import mCommonDi._
  import esModel.api._

  val sysAdRenderUtil: SysAdRenderUtil


  import sysAdRenderUtil._

  /**
   * Рендер страницы с формой забивания значений [[models.blk.OneAdQsArgs]].
   *
   * @param madId id текущей рекламной карточки.
   * @param rvar Интересующий render variant.
   * @return 200: Страница с формой.
   *         Редиректы в остальных случаях.
   *         404 если указанная карточка не найдена.
   */
  def showOneAdForm(madId: String, rvar: OneAdRenderVariant) = csrf.AddToken {
    isSuMad(madId).async { implicit request =>
      // Забиндить форму дефолтовыми данными для отправки в шаблон.
      val formArgs = OneAdQsArgs(
        adId    = madId,
        szMult  = 1.0F,
        vsnOpt  = request.mad.versionOpt,
        imgFmt  = MImgFormats.JPEG,
        wideOpt = for (bm <- request.mad.ad.blockMeta) yield {
          OneAdWideQsArgs(
            width = bm.width * 2
          )
        }
      )
      val qf = oneAdQsArgsFormM(madId)
        .fill( formArgs )
      // Запустить рендер.
      _showOneAdFormRender(qf, rvar, Ok)
    }
  }

  private def _showOneAdFormRender(qf: Form[OneAdQsArgs], rvar: OneAdRenderVariant, rs: Status)
                                  (implicit request: IAdReq[_]): Future[Result] = {
    val producerIdOpt = n2NodesUtil.madProducerId(request.mad)
    val nodeOptFut = mNodes.maybeGetByIdCached( producerIdOpt )
    for {
      nodeOpt <- nodeOptFut
    } yield {
      val rargs = MShowOneAdFormTplArgs(request.mad, rvar, qf, nodeOpt)
      val html = argsFormTpl(rargs)
      rs( html )
    }
  }


  /**
   * Сабмит формы запроса рендера карточки.
   *
   * @param madId id карточки.
   * @param rvar Интересующий render variant.
   * @return Редирект на результат рендера карточки согласно переданным параметрам.
   */
  def oneAdFormSubmit(madId: String, rvar: OneAdRenderVariant) = csrf.Check {
    isSuMad(madId).async { implicit request =>
      oneAdQsArgsFormM(madId).bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"oneAdFormSubmit($madId, ${rvar.nameI18n}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _showOneAdFormRender(formWithErrors, rvar, NotAcceptable)
        },
        {oneAdQsArgs =>
          val call = _oneAdRenderCall(rvar, oneAdQsArgs)
          Redirect( call )
        }
      )
    }
  }


  /** Сборка ссылки требуемый на рендер карточки.
    *
    * @param rvar Вариант рендера.
    * @param qsArgs Парамерты рендера.
    * @return Инстанс Call.
    */
  private def _oneAdRenderCall(rvar: OneAdRenderVariant, qsArgs: OneAdQsArgs): Call = {
    rvar match {
      case OneAdRenderVariants.ToHtml =>
        routes.Sc.onlyOneAd( qsArgs )
      case OneAdRenderVariants.ToImage =>
        routes.Sc.onlyOneAdAsImage( qsArgs )
    }
  }

}
