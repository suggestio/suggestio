package controllers

import models.MAdnNodeCache
import models.blk.{OneAdWideQsArgs, OneAdQsArgs}
import models.im.OutImgFmts
import models.msc.OneAdRenderVariant
import play.api.data.{Mapping, Form}
import play.twirl.api.Html
import util.{PlayMacroLogsI, FormUtil}
import util.acl.{RequestWithAd, IsSuperuserMadPost, IsSuperuserMadGet}
import views.html.sys1.market.ad.one._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 15:50
 * Description: sys-раздел для отладки рендера карточек в картинки или html.
 */
object SysAdRender {

  import play.api.data.Forms._

  /** Внутренний маппинг контроллера для OneAdQsArgs. */
  def oneAdQsArgsM(madId: String): Mapping[OneAdQsArgs] = {
    mapping(
      "szMult" -> FormUtil.szMultM,
      "vsn"    -> FormUtil.esVsnOptM,
      "imgFmt" -> OutImgFmts.mapping,
      "wide"   -> OneAdWideQsArgs.optMapper
    )
    { OneAdQsArgs(madId, _, _, _, _) }
    { OneAdQsArgs.unapply(_).map {
        case (_, a, b, c, d) => (a, b, c, d)
    }}
  }

  /** Маппинг для формы OneAdQsArgs. */
  def oneAdQsArgsFormM(madId: String): Form[OneAdQsArgs] = {
    Form( oneAdQsArgsM(madId) )
  }

}


import SysAdRender._


/** Аддон для sys-контроллера для добавления экшенов, связанных с рендером карточки. */
trait SysAdRender extends SioController with PlayMacroLogsI {

  /**
   * Рендер страницы с формой забивания значений [[models.blk.OneAdQsArgs]].
   * @param madId id текущей рекламной карточки.
   * @param rvar Интересующий render variant.
   * @return 200: Страница с формой.
   *         Редиректы в остальных случаях.
   *         404 если указанная карточка не найдена.
   */
  def showOneAdForm(madId: String, rvar: OneAdRenderVariant) = IsSuperuserMadGet(madId).async { implicit request =>
    // Забиндить форму дефолтовыми данными для отправки в шаблон.
    val qf = oneAdQsArgsFormM(madId)
      .fill(OneAdQsArgs(
        adId    = madId,
        szMult  = 1.0F,
        vsnOpt  = request.mad.versionOpt,
        imgFmt  = OutImgFmts.JPEG,
        wideOpt = Some(OneAdWideQsArgs(
          width = request.mad.blockMeta.width * 2
        ))
      ))
    // Запустить рендер.
    _showOneAdFormRender(qf, rvar)
      .map { Ok(_) }
  }

  private def _showOneAdFormRender(qf: Form[OneAdQsArgs], rvar: OneAdRenderVariant)(implicit request: RequestWithAd[_]): Future[Html] = {
    val nodeOptFut = MAdnNodeCache.getById( request.mad.producerId )
    for {
      nodeOpt <- nodeOptFut
    } yield {
      argsFormTpl(
        mad     = request.mad,
        rvar    = rvar,
        qf      = qf,
        nodeOpt = nodeOpt
      )
    }
  }

  /**
   * Сабмит формы запроса рендера карточки.
   * @param madId id карточки.
   * @param rvar Интересующий render variant.
   * @return Редирект на результат рендера карточки согласно переданным параметрам.
   */
  def oneAdFormSubmit(madId: String, rvar: OneAdRenderVariant) = IsSuperuserMadPost(madId).async { implicit request =>
    oneAdQsArgsFormM(madId).bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"oneAdFormSubmit($madId, ${rvar.nameI18n}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        _showOneAdFormRender(formWithErrors, rvar)
          .map { NotAcceptable(_) }
      },
      {oneAdQsArgs =>
        Redirect( rvar.routesCall(oneAdQsArgs) )
      }
    )
  }

}
