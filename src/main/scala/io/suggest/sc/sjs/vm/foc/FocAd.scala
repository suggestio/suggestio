package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.m.mdom.content.{NodesHtmlContent, StrHtmlContent, IHtmlContent}
import io.suggest.sc.sjs.m.msrv.foc.find.IFocAd
import io.suggest.sc.sjs.vm.util.cont.ContainerT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:11
 * Description: ViewModel для одной focused-карточки в отображаемой выдаче.
 * Карточка может быть как загружена, так и выгружена (не загружена):
 * Это описывается внутренним состоянием, и при изменениям сразу синхронизируется в DOM.
 */

object FocAd {

  /**
   * Сборка нового экземпляра на основе полученного от сервера экземпляра MFocAd.
   * @return Экземпляр [[FocAd]].
   */
  def apply(html: String): FocAd = {
    apply(StrHtmlContent(html))
  }
  def apply(html: IHtmlContent): FocAd = {
    apply(Left(html))
  }

}


/** Экземпляр ViewModel для одной карточки.
  * @param shown состояние отображения:
  *               Left: скрыто и есть верстка вне DOM.
  *               Right - отображено в указанном контейнере.
  */
case class FocAd(
  private var shown: Either[IHtmlContent, ContainerT]
) {

  /** Убедиться, что верстка карточки залита в DOM. */
  def becomeRendered(container: ContainerT): Unit = {
    for (html <- shown.left) {
      html.writeInto(container._underlying)
      shown = Right(container)
    }
  }

  /** Убедиться, что верстка карточки отсутсвует в DOM. */
  def unrender(): Unit = {
    for (cont <- shown.right) {
      shown = Left( NodesHtmlContent(cont._underlying.children) )
      cont.clear()
    }
  }

}

