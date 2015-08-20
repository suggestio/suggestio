package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.m.msrv.foc.find.{MFocAdImpl, IFocAd}
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.15 17:21
 * Description: Модель "отображаемой" focused-карточки. Отображаемость может быть так же и за экраном.
 * По сути тут контейнер, связывающий метаданные карточки и контент, уже залитый в DOM.
 */
object FAdShown {

  def apply(fadRoot: FAdRoot, meta: IFocAd): FAdShown = {
    apply(fadRoot, meta.controlsHtml, meta.producerId, meta.madId, meta.index)
  }

}


/**
 * Класс модели.
 * @param fadRoot root div карточки, уже залитой в DOM.
 * @param controlsHtml доступ к верстке controls (arrows, header).
 *                        Эта верстка или тут в виде строки (offscreen-карточка),
 *                        или уже в DOM залита внутри vm'ки FControls (текущая карточка).
 * @param producerId es id продьюсера карточки.
 * @param madId es id карточки.
 * @param index внутренний порядковый номер карточки.
 */
case class FAdShown(
  fadRoot         : FAdRoot,
  controlsHtml    : String,
  producerId      : String,
  madId           : String,
  index           : Int
) extends IFocAd {

  override def bodyHtml: String = {
    fadRoot.outerHtml
  }

  /** Изъятие из отображения в карусели текущей карточки. */
  def unshow(): MFocAdImpl = {
    fadRoot.remove()
    mFocAdImpl
  }

}
