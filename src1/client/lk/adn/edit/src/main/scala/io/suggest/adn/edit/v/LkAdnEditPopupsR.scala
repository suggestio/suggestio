package io.suggest.adn.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.m.MLkAdnEditRoot
import io.suggest.lk.m.{MErrorPopupS, MFormResourceKey}
import io.suggest.lk.pop.PopupsContR
import io.suggest.lk.r.ErrorPopupR
import io.suggest.lk.r.crop.CropPopupR
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.04.18 18:26
  * Description: Компонент попапов формы редактирования узла.
  */
class LkAdnEditPopupsR(
                        val cropPopupR: CropPopupR
                      ) {

  type Props = ModelProxy[MLkAdnEditRoot]

  import MErrorPopupS.MErrorPopupSFastEq
  import cropPopupR.CropPopupPropsFastEq
  import PopupsContR.PopContPropsValFastEq


  protected[this] case class State(
                                    popupsContPropsC    : ReactConnectProxy[PopupsContR.PropsVal],
                                    errorPopupC         : ReactConnectProxy[Option[MErrorPopupS]],
                                    cropPopupC          : ReactConnectProxy[cropPopupR.Props_t]
                                  )

  class Backend($: BackendScope[Props, State]) {
    def render(s: State): VdomElement = {
      val popupsVdom = Seq[VdomNode](
        // Попап ошибки.
        s.errorPopupC { ErrorPopupR.apply },

        // Попап кропа картинки.
        s.cropPopupC { cropPopupR.apply }
      )

      s.popupsContPropsC {
        PopupsContR(_)( popupsVdom: _* )
      }
    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .initialStateFromProps { propsProxy =>
      State(
        popupsContPropsC = propsProxy.connect { mroot =>
          PopupsContR.PropsVal(
            visible = mroot.popups.nonEmpty
          )
        },

        errorPopupC = propsProxy.connect(_.popups.errorPopup)( OptFastEq.Wrapped ),

        cropPopupC = {
          propsProxy.connect { root =>
            for {
              mcrop       <- root.popups.cropPopup
              edge        <- root.node.edges.get( mcrop.imgEdgeUid )
              imgSrc      <- edge.origImgSrcOpt
            } yield {
              cropPopupR.PropsVal(
                imgSrc      = imgSrc,
                percentCrop = mcrop.percentCrop,
                popCssClass = Nil, //lkAdEditCss.Crop.popup.htmlClass :: Nil,
                resKey      = MFormResourceKey(
                  pred      = None,
                  edgeUid   = Some( edge.jdEdge.id ),
                  nodePath  = None
                )
              )
            }
          }( OptFastEq.Wrapped )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(popupsProxy: Props) = component( popupsProxy )

}