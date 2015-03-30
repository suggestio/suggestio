package models.adv.ext.act

import controllers.routes
import io.suggest.adv.ext.model.im.INamedSize2di
import models.{Context, MImgSizeT}
import models.adv.js.ctx.MPictureCtx
import models.blk.{OneAdQsArgs, SzMult_t, OneAdWideQsArgs}
import util.blocks.BgImg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.15 14:59
 * Description: Модель, живущая внутри актора [[util.adv.ExtTargetActor]], позволяющая получить/задавать
 * определяемые клиентом данные, и иные данные, зависимые от исходных.
 * Из-за необходимости доступа модели к окружению актора, модель живёт внутри трейта [[ExtTargetActorEnv]].
 *
 * Это шаблон финальной модели. Её нужно завернуть в классы для финальной реализации.
 */

trait EtaCustomArgsBase extends ExtTargetActorEnv {

  /** Абстрактная реализация модели в виде трейта. */
  protected trait MCustomArgsT {

    /** Размер дял рендера. */
    def adRenderMaxSz: INamedSize2di

    /** Инфа по рендеру карточки в картинке. */
    lazy val madRenderInfo: PicInfo = {
      // Вычисляем мультипликатор размера исходя из отношения высот.
      val srv = service
      val tgUrl = args.target.target.url
      val sz = adRenderMaxSz
      val hDiff = sz.height.toFloat / mad.blockMeta.height.toFloat
      // Нужно домножить на минимально необходимый размер для сервиса.
      // TODO Проквантовать полученный szMult?
      val szMultV = hDiff * srv.szMult
      // Вычислить необходимость и ширину широкого отображения.
      // 2015.mar.13: Запрет для wide-рендера карточек без картинки. Пока эта функция не работает как надо.
      val wideWidthOpt = BgImg.getBgImg(mad)
        .flatMap { _ => srv.advExtWidePosting(tgUrl, mad) }
        //.filter { pmWidth => mad.blockMeta.wide || pmWidth.toFloat > mad.blockMeta.width * 1.15F }
      PicInfo(
        wide   = wideWidthOpt,
        width  = wideWidthOpt.fold { BgImg.szMulted(mad.blockMeta.width, srv.szMult) } (_.width),
        height = BgImg.szMulted(sz.height, szMultV),
        szMult = szMultV,
        stdSz  = sz
      )
    }

    /**
     * Сгенерить аргументы для рендера карточки в картинку.
     * @return Экземпляр [[models.blk.OneAdQsArgs]], готовый к эксплуатации.
     */
    def adRenderArgs = {
      val mri = madRenderInfo
      OneAdQsArgs(
        adId    = args.qs.adId,
        szMult  = mri.szMult,
        vsnOpt  = mad.versionOpt,
        imgFmt  = service.imgFmt,
        wideOpt = mri.wide
      )
    }

    /** JSON-контекст инфы по картинке текущей карточки. */
    def jsPicCtx: MPictureCtx = {
      val mri = madRenderInfo
      val url = Context.SC_URL_PREFIX + routes.MarketShowcase.onlyOneAdAsImage(adRenderArgs).url
      MPictureCtx(
        size   = Some(mri.stdSz.szAlias),
        sioUrl = Some(url)
      )
    }

  }

  /** custom-аргументы являются глобальными, поэтому к ним доступ из актора отовсюду. */
  protected def _customArgs: MCustomArgsT

}


/**
 * Инфа по картинке кодируется этим классом.
 * @param wide рендерить широко. С указанной шириной.
 * @param width Ширина оригинальная отмасштабированная.
 * @param height Высота оригинальная отмасштабированная.
 * @param szMult Множитель масштабирования оригинала.
 * @param stdSz Штатный размер, по которому равнялись.
 */
case class PicInfo(
  wide    : Option[OneAdWideQsArgs],
  width   : Int,
  height  : Int,
  szMult  : SzMult_t,
  stdSz   : INamedSize2di
)
  extends MImgSizeT
