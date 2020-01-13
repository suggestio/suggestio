package models.adv.ext

import io.suggest.common.geom.d2.{INamedSize2di, MSize2di}
import io.suggest.n2.node.MNode
import models.blk.OneAdQsArgs
import models.blk.szMulted
import util.adv.IAdvUtilDi
import util.ext.IExtServiceHelper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 19:06
 * Description: Калькулятор для рендера ссылки на рендер карточки. Сама калькуляция происходит при обращении к ссылке,
 * тут лишь рассчет ссылки на картинку.
 */
trait Mad2ImgUrlCalcOuter extends IAdvUtilDi {

  /** Непосредственный код калькулятора. */
  trait Mad2ImgUrlCalcT {

    /** Размер для рендера. */
    def adRenderMaxSz: INamedSize2di

    def serviceHelper: IExtServiceHelper

    def tgUrl: String

    def mad: MNode

    def adId = mad.id.get

    /** Инфа по рендеру карточки в картинке. */
    def madRenderInfo: PicInfo = {
      // Вычисляем мультипликатор размера исходя из отношения высот.
      val srv = serviceHelper
      val sz = adRenderMaxSz
      val jdtP1 = advUtil.getAdvMainBlock(mad)
        .get
        .rootLabel
        .props1
      val widthPx  = jdtP1.widthPx.get
      val heightPx = jdtP1.heightPx.get
      val whSzM = Math.min(
        sz.whPx.height.toFloat / heightPx,
        sz.whPx.width.toFloat  / widthPx,
      )
      // Нужно домножить на минимально необходимый размер для сервиса.
      // TODO Проквантовать полученный szMult?
      val szMultV = whSzM * srv.szMult
      // Вычислить необходимость и ширину широкого отображения.
      // 2015.mar.13: Запрет для wide-рендера карточек без картинки. Пока эта функция не работает как надо.
      val wideWidthOpt = srv.advExtWidePosting(tgUrl, mad)
      //.filter { pmWidth => mad.blockMeta.wide || pmWidth.toFloat > mad.blockMeta.width * 1.15F }
      PicInfo(
        wide   = wideWidthOpt,
        wh = MSize2di(
          width  = wideWidthOpt.fold {
            szMulted(widthPx, srv.szMult)
          }(_.width),
          height = szMulted(sz.whPx.height, szMultV)
        ),
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
        adId    = adId,
        szMult  = mri.szMult,
        vsnOpt  = mad.versionOpt,
        imgFmt  = serviceHelper.imgFmt,
        wideOpt = mri.wide
      )
    }

  }


  abstract class Mad2ImgUrlCalc
    extends Mad2ImgUrlCalcT

}
