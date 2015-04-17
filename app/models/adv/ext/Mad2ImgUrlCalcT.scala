package models.adv.ext

import io.suggest.adv.ext.model.im.INamedSize2di
import models.MAd
import models.blk.OneAdQsArgs
import models.mext.MExtService
import util.blocks.BgImg
import models.blk.szMulted

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 19:06
 * Description: Калькулятор для рендера ссылки на рендер карточки. Сама калькуляция происходит при обращении к ссылке,
 * тут лишь рассчет ссылки на картинку.
 */
trait Mad2ImgUrlCalcT {

  /** Размер для рендера. */
  def adRenderMaxSz: INamedSize2di

  def service: MExtService

  def tgUrl: String

  def mad: MAd

  def adId = mad.id.get

  /** Инфа по рендеру карточки в картинке. */
  def madRenderInfo: PicInfo = {
    // Вычисляем мультипликатор размера исходя из отношения высот.
    val srv = service
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
      width  = wideWidthOpt.fold { szMulted(mad.blockMeta.width, srv.szMult) } (_.width),
      height = szMulted(sz.height, szMultV),
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
      imgFmt  = service.imgFmt,
      wideOpt = mri.wide
    )
  }

}
