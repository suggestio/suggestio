package models.adv.ext.act

import controllers.routes
import io.suggest.common.geom.d2.INamedSize2di
import models.adv.ext.Mad2ImgUrlCalcOuter
import models.adv.js.ctx.MPictureCtx
import models.mctx.IContextUtilDi

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

trait EtaCustomArgsBase
  extends ExtTargetActorEnv
  with IContextUtilDi
  with Mad2ImgUrlCalcOuter
{ env =>

  def _adRenderMaxSzDflt: INamedSize2di = {
    serviceHelper.advPostMaxSz(args.target.target.url)
  }

  /** Абстрактная реализация модели в виде трейта. */
  protected class MCustomArgs extends Mad2ImgUrlCalc {

    /** Размер дял рендера. */
    def adRenderMaxSz: INamedSize2di = _adRenderMaxSzDflt

    override def serviceHelper = env.serviceHelper
    override def mad     = env.args.request.mad
    override def tgUrl   = env.args.target.target.url

    /** JSON-контекст инфы по картинке текущей карточки. */
    def jsPicCtx: MPictureCtx = {
      val mri = madRenderInfo
      val url = ctxUtil.SC_URL_PREFIX + routes.Sc.onlyOneAdAsImage(adRenderArgs).url
      MPictureCtx(
        size   = Some(mri.stdSz.szAlias),
        sioUrl = Some(url)
      )
    }

  }

}

