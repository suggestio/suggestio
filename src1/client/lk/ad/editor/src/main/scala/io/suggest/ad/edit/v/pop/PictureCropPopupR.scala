package io.suggest.ad.edit.v.pop

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop, ReactCrop, ReactCropProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{CropCancel, CropChanged, CropSave}
import io.suggest.lk.pop.PopupR
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.ReactImageCropUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 16:48
  * Description: React-компонент для кропа картинки.
  */
class PictureCropPopupR(
                         laeCss: LkAdEditCss
                       ) {

  case class PropsVal(
                       imgSrc       : String,
                       percentCrop  : PercentCrop
                     )
  implicit object PictureCropPopupPropsFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.imgSrc ===* b.imgSrc) &&
        (a.percentCrop ===* b.percentCrop)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, CropCancel )
    }

    private def saveBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, CropSave )
    }

    private def onCropChange( pcCrop: PercentCrop, pxCrop: PixelCrop ): Callback = {
      dispatchOnProxyScopeCB( $, CropChanged(pcCrop, pxCrop) )
    }
    private val onCropChangeF = ReactCommonUtil.cbFun2ToJsCb( onCropChange )


    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        propsOptProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(closeBtnClick),
            css       = laeCss.Crop.popup.htmlClass :: Nil,
            topPc     = 10,
            tagMod    = Some {
              ^.right := (-19).pct
            }
          )
        } { popupPropsProxy =>
          val B = Css.Buttons
          PopupR(popupPropsProxy)(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Picture.editing` )
            ),

            <.div(
              ReactCrop(
                new ReactCropProps {
                  override val src          = props.imgSrc
                  override val crop         = props.percentCrop
                  override val onComplete   = onCropChangeF
                  // TODO Соотнести minWH с percentCrop или aspect, чтобы не было скачков на экране.
                  override val minHeight    = 50
                  override val minWidth     = 50
                }
              )
            ),

            <.a(
              ^.`class` := Css.flat( B.BTN, B.BTN_W, B.MAJOR, Css.Size.M ),
              ^.onClick --> saveBtnClick,
              Messages( MsgCodes.`Apply` )
            ),

            HtmlConstants.NBSP_STR,
            HtmlConstants.NBSP_STR,

            <.a(
              ^.`class` := Css.flat( B.BTN, B.BTN_W, B.NEGATIVE, Css.Size.M ),
              ^.onClick --> closeBtnClick,
              Messages( MsgCodes.`Cancel` )
            ),
            <.br
          )
        }
      }
    }
  }


  val component = ScalaComponent.builder[Props]("CropPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
