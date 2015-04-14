package models.adv.ext.act

import controllers.routes
import io.suggest.adv.ext.model.im.INamedSize2di
import models.adv.ext.Mad2ImgUrlCalcT
import models.Context
import models.adv.js.ctx.MPictureCtx

// TODO Модель довольно странная, её основная логика вынесена в Mad2ImgUrlCalcT, а тут какие-то непонятные ошметки остались.
// Возможно, эту модель надо заинлайнить и удалить.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.15 14:59
 * Description: Модель, живущая внутри adv-ext-target-акторов и позволяющая получить/задавать
 * определяемые клиентом данные, и иные данные, зависимые от исходных.
 * Из-за необходимости доступа модели к окружению актора, модель живёт внутри трейта [[ExtTargetActorEnv]].
 *
 * Это шаблон финальной модели. Её нужно завернуть в классы для финальной реализации.
 */

trait EtaCustomArgsBase extends ExtTargetActorEnv { env =>

  def _adRenderMaxSzDflt = service.advPostMaxSz(args.target.target.url)

  /** Абстрактная реализация модели в виде трейта. */
  protected trait MCustomArgsT extends Mad2ImgUrlCalcT {

    /** Размер дял рендера. */
    def adRenderMaxSz: INamedSize2di = _adRenderMaxSzDflt

    override def service = env.service
    override def mad     = env.args.request.mad
    override def tgUrl   = env.args.target.target.url

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

}

