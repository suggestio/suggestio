package io.suggest.ad.edit.v.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.empty.OptionUtil
import io.suggest.form.MFormResourceKey
import io.suggest.lk.m.{MDeleteConfirmPopupS, MErrorPopupS}
import io.suggest.lk.r.img.CropPopupR
import io.suggest.lk.r.popup.PopupsContR
import io.suggest.lk.r.{DeleteConfirmPopupR, ErrorPopupR}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 16:27
  * Description: Lk Ad Edit Popups -- все попапы формы живут здесь.
  */
final class LaePopupsR(
                        deleteConfirmPopupR: DeleteConfirmPopupR,
                        lkAdEditCss     : LkAdEditCss,
                        errorPopupR     : ErrorPopupR,
                        val cropPopupR  : CropPopupR
                      ) {

  import cropPopupR.CropPopupPropsFastEq
  import MErrorPopupS.MErrorPopupSFastEq
  import PopupsContR.PopContPropsValFastEq
  import MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq

  type Props = ModelProxy[MAeRoot]

  protected case class State(
                              popupsVisibleSomeC  : ReactConnectProxy[Some[Boolean]],
                              errorMsgsC          : ReactConnectProxy[Option[MErrorPopupS]],
                              cropPopPropsOptC    : ReactConnectProxy[Option[cropPopupR.PropsVal]],
                            )

  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      val popupContBody = List[VdomNode](

        // Попап кропа картинки:
        s.cropPopPropsOptC { cropPopupR.component.apply },

      )

      React.Fragment(

        // Попап с ~отрендеренными ошибками:
        s.errorMsgsC { errorPopupR.component.apply },

        // Попап подтверждения удаления рекламной карточки.
        p.wrap( _.popups.deleteConfirm )( deleteConfirmPopupR.component.apply ),

        s.popupsVisibleSomeC { popupContProps =>
          popupContProps.wrap { visibleSome =>
            PopupsContR.PropsVal(
              visible = visibleSome.value,
            )
          }( PopupsContR(_)( popupContBody: _* ) )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { rootProxy =>
      State(

        popupsVisibleSomeC = rootProxy.connect { mroot =>
          val p = mroot.popups
          OptionUtil.SomeBool( p.error.nonEmpty || p.pictureCrop.nonEmpty )
        },

        errorMsgsC = rootProxy.connect { _.popups.error },

        cropPopPropsOptC = rootProxy.connect { root =>
          for {
            mcrop       <- root.popups.pictureCrop
            edge        <- root.doc.jdDoc.jdArgs.data.edges
              .get( mcrop.imgEdgeUid )
            imgSrc      <- edge.origImgSrcOpt
          } yield {
            cropPopupR.PropsVal(
              imgSrc      = imgSrc,
              percentCrop = mcrop.percentCrop,
              popCssClass = lkAdEditCss.Crop.popup.htmlClass :: Nil,
              resKey      = MFormResourceKey(
                edgeUid   = edge.jdEdge.edgeDoc.id,
                nodePath  = root.doc.jdDoc.jdArgs.renderArgs.selPath,
              ),
              withDelete  = false,
            )
          }
        },

      )
    }
    .renderBackend[Backend]
    .build

}
