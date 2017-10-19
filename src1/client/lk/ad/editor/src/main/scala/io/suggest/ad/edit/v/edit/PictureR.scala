package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.{CropOpen, PictureFileChanged}
import io.suggest.color.MHistogram
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.file.up.MFileUploadS
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.spa.OptFastEq
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sjs.common.i18n.Messages
import diode.react.ReactPot._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 17:14
  * Description: Элемент формы для управления картинкой-изображением.
  * Изначально -- только из файла.
  */
class PictureR(
                val colorsSuggestR        : ColorsSuggestR
              ) {

  import MFileUploadS.MFileUploadSFastEq
  import colorsSuggestR.ColorsSuggestPropsValFastEq

  case class PropsVal(
                       imgSrcOpt      : Option[String],
                       uploadState    : Option[MFileUploadS],
                       histogram      : Option[MHistogram]
                     )
  implicit object PictureRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.imgSrcOpt      ===* b.imgSrcOpt) &&
        (a.uploadState  ===* b.uploadState) &&
        (a.histogram    ===* b.histogram)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]

  protected[this] case class State(
                                    imgSrcOptC        : ReactConnectProxy[Option[String]],
                                    upStateOptC       : ReactConnectProxy[Option[MFileUploadS]],
                                    colSuggPropsOptC  : ReactConnectProxy[Option[colorsSuggestR.PropsVal]]
                                  )

  class Backend($: BackendScope[Props, State]) {

    /** Реакция на выбор файла в file input. */
    private def _onFileChange(e: ReactEventFromInput): Callback = {
      val files = DomListSeq( e.target.files )
      dispatchOnProxyScopeCB($, PictureFileChanged(files))
    }

    /** Клик по кнопке удаления файла. */
    private def _onDeleteFileClick: Callback = {
      dispatchOnProxyScopeCB($, PictureFileChanged(Nil))
    }

    /** Клик по кнопке кадрирования картинки. */
    private def _onCropClick: Callback = {
      dispatchOnProxyScopeCB($, CropOpen)
    }


    def render(p: Props, s: State): VdomElement = {
      val C = Css.Lk.AdEdit.Image
      // Отрендерить текущую картинку
      p.value.whenDefinedEl { _ =>
        <.div(
          ^.`class` := Css.Overflow.HIDDEN,

          // Рендер цветов текущей картинки
          s.colSuggPropsOptC { colorsSuggestR.apply },

          // Рендерить картинку и управление ей:
          s.imgSrcOptC { imgSrcOptProxy =>
            val imgSrcOpt = imgSrcOptProxy.value
            <.div(

              <.div(
                ^.`class` := Css.flat( C.IMAGE, Css.Size.M ),

                imgSrcOpt.fold[VdomElement] {
                  // Картинки нет. Можно загрузить файл.
                  <.input(
                    ^.`type`    := HtmlConstants.Input.file,
                    ^.onChange ==> _onFileChange
                  )

                } { blobUrl =>
                  <.div(
                    // thumbnail картинки.
                    <.img(
                      ^.src := blobUrl,
                      ^.width := 140.px
                      //^.height := 85.px   // Чтобы не плющило картинки, пусть лучше обрезает снизу
                    ),

                    // Ссылка-кнопка удаления картинки.
                    <.a(
                      ^.`class` := C.IMAGE_REMOVE_BTN,
                      ^.title   := Messages( MsgCodes.`Delete` ),
                      ^.onClick --> _onDeleteFileClick,
                      <.span
                    )
                  )
                }
              ),

              // Кнопка для кропа изображения.
              imgSrcOpt.whenDefined { _ =>
                val B = Css.Buttons
                <.a(
                  ^.`class` := Css.flat( B.BTN, B.BTN_W, B.MINOR, Css.Size.M ),
                  ^.onClick --> _onCropClick,
                  Messages( MsgCodes.`Crop` ), HtmlConstants.ELLIPSIS
                )
              }

            )
          },

          // Отрендерить данные процесса загрузки:
          s.upStateOptC { upStateOptProxy =>
            upStateOptProxy.value.whenDefinedEl { upState =>
              <.div(

                if ( upState.uploadReq.isPending || upState.prepareReq.isPending ) {
                  <.span(
                    LkPreLoaderR.AnimSmall,

                    Messages( MsgCodes.`Uploading.file` ),
                    HtmlConstants.ELLIPSIS
                  )
                } else {
                  EmptyVdom
                },

                upState.progress.whenDefined { progress =>
                  <.span(
                    `(`, progress.pct, `)`
                  )
                },

                //upState.prepareReq
                _renderReqErr( upState.prepareReq, MsgCodes.`Preparing` ),
                _renderReqErr( upState.uploadReq, MsgCodes.`Uploading.file` ),

              )
            }
          }

        )
      }
    }

  }

  private def _renderReqErr(reqPot: Pot[_], commentCode: String): TagMod = {
    reqPot.renderFailed { ex =>
      val SPACE = HtmlConstants.SPACE
      <.span(
        ^.`class` := Css.Colors.RED,
        Messages( MsgCodes.`Error` ), HtmlConstants.COLON, SPACE,
        `(`, Messages( commentCode ), `)`, SPACE,

        ex.toString,
        HtmlConstants.COLON, SPACE,
        ex.getMessage
      )
    }
  }

  val component = ScalaComponent.builder[Props]("Pic")
    .initialStateFromProps { propsOptProxy =>
      State(

        imgSrcOptC = propsOptProxy.connect { propsOpt =>
          propsOpt.flatMap(_.imgSrcOpt)
        }(OptFastEq.Plain),

        upStateOptC = propsOptProxy.connect { propsOpt =>
          propsOpt.flatMap(_.uploadState)
        }(OptFastEq.Wrapped),

        colSuggPropsOptC = propsOptProxy.connect { propsOpt =>
          for {
            props <- propsOpt
            hist  <- props.histogram
          } yield {
            colorsSuggestR.PropsVal(
              titleMsgCode = MsgCodes.`Suggested.bg.colors`,
              colors       = hist.sorted
            )
          }
        }( OptFastEq.Wrapped )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
