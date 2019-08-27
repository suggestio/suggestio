package io.suggest.lk.r.img

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.common.geom.d2.ISize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEdgeId
import io.suggest.lk.m.{CropOpen, PictureFileChanged}
import io.suggest.lk.m.frk.MFormResourceKey
import io.suggest.msg.Messages
import io.suggest.n2.edge.MEdgeDataJs
import MEdgeDataJs.MEdgeDataJsTupleFastEq
import io.suggest.dev.{MSzMult, MSzMults}
import io.suggest.lk.r.FilesDropZoneR
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import io.suggest.sjs.common.model.dom.DomListSeq
import io.suggest.spa.DAction
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.18 10:36
  * Description: Кнопка редактора какой-то картинки: картинку можно загрузить, убрать, кропнуть и т.д.
  */
class ImgEditBtnR(
                   val filesDropZoneR   : FilesDropZoneR,
                   imgRenderUtilJs      : ImgRenderUtilJs
                 ) {


  type Props_t = ImgEditBtnPropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на выбор файла в file input. */
    private def _onFileChange(e: ReactEventFromInput): Callback = {
      // Тут передаётся hosted-object внутри сообщения, но вроде бы это безопасно и не глючит нигде (files)
      _picFileChanged( DomListSeq(e.target.files) )
    }

    /** Клик по кнопке удаления файла. */
    private lazy val _onDeleteFileClick: Callback = {
      _picFileChanged( Nil )
    }

    /** Клик для запуска кадрирования. */
    private lazy val _onCropClick: Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        val v = props.value
        CropOpen( v.resKey, v.cropOnClick.get )
      }
    }

    private def _picFileChanged(files: Seq[dom.File]): Callback = {
      dispatchOnProxyScopeCBf($) { props: Props =>
        PictureFileChanged(files, props.value.resKey)
      }
    }

    /** Реакция на img.onLoad. */
    private def _onImgLoaded(e: ReactEvent): Callback = {
      $.props >>= { props: Props =>
        props.value.edge.fold(Callback.empty) { edge =>
          imgRenderUtilJs.notifyImageLoaded($, edge._1.edgeUid, e)
        }
      }
    }

    def render(propsValProxy: Props): VdomElement = {
      val C = Css.Lk.Image
      val propsVal = propsValProxy.value
      println("IEB render(): " + propsVal.resKey)

      <.div(
        ^.`class` := Css.flat1( C.IMAGE :: propsVal.size :: Css.Overflow.HIDDEN :: propsVal.css ),

        propsVal.bgColor.whenDefined { bgColor =>
          ^.backgroundColor := bgColor.hexCode
        },

        propsVal.edge.flatMap(_._2.imgSrcOpt).fold[VdomElement] {
          val upCss = Css.Lk.Uploads
          // Картинки нет. Можно загрузить файл.
          <.div(
            <.input(
              ^.`type`    := HtmlConstants.Input.file,
              ^.`class`   := upCss.ADD_FILE_INPUT,
              ^.onChange ==> _onFileChange,
            ),

            <.a(
              ^.`class` := upCss.IMAGE_ADD_BTN,
              <.span(
                Messages( MsgCodes.`Upload.file`, HtmlConstants.SPACE )
              )
            )
          )

        } { blobUrl =>
          val width0 = 140
          val height0 = 85
          val cropOpt = propsVal.edge.flatMap(_._1.crop)

          <.div(
            // thumbnail картинки.
            <.img(
              ^.src := blobUrl,

              // Если нет данных о размерах картинки, то повесить событие onLoad
              ReactCommonUtil.maybe( propsVal.edge.exists(_._2.fileJs.exists(_.whPx.isEmpty)) ) {
                ^.onLoad ==> _onImgLoaded
              },

              // Имитатор кропа на клиенте, если требуется:
              imgRenderUtilJs.htmlImgCropEmuAttrsOpt(
                cropOpt    = cropOpt,
                outerWhOpt = propsVal.cropOnClick,
                origWhOpt  = propsVal.edge.flatMap(_._2.origWh),
                szMult     = propsVal.cropOnClick.fold( MSzMults.`1.0` ) { coc =>
                  MSzMult.fromDouble( width0.toDouble / coc.width.toDouble )
                }
              ).fold[TagMod](
                ^.width := width0.px
                //^.height := height0.px   // Чтобы не плющило картинки, пусть лучше обрезает снизу
              ) { croppedTm =>
                TagMod(
                  ^.height := propsVal
                    .cropOnClick
                    .filter(_ => cropOpt.isDefined)
                    .fold(height0) { coc =>
                      (width0.toDouble / coc.width * coc.height).toInt
                    }.px,
                  croppedTm
                )
              }
            ),

            // Ссылка-кнопка удаления картинки.
            <.a(
              ^.`class` := propsVal.cropOnClick.fold(C.IMAGE_REMOVE_BTN)(_ => C.IMAGE_EDIT_BTN),
              ^.title   := Messages( propsVal.cropOnClick.fold(MsgCodes.`Delete`)(_ => MsgCodes.`Edit`) ),
              ^.onClick --> propsVal.cropOnClick.fold(_onDeleteFileClick)(_ => _onCropClick),
              <.span
            )
          )
        }
      )
    }

  }


  /** Компонент без поддержки Drag-Drop файлов. */
  val componentPlain = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ) )
    .build


  sealed case class StateDrop(
                               plainC: ReactConnectProxy[Props_t],
                               fileDropC: ReactConnectProxy[Props_t],
                             )

  /** Компонент с поддержкой Drag-Drop файлов извне. Требует DndContext снаружи. */
  val componentDrop = ScalaComponent
    .builder[Props]( getClass.getSimpleName + "Dnd" )
    .initialStateFromProps { propsProxy =>
      println("dnd initState()")
      StateDrop(
        plainC    = propsProxy.connect(identity)( ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ),
        fileDropC = propsProxy.connect(identity)( ImgEditBtnPropsVal.ImgEditBtnRPropsVal4DropZoneFastEq ),
      )
    }
    .render_S { s =>
      println("dnd RENDER()")
      val inner = s.plainC( componentPlain.apply )
      s.fileDropC { propsProxy =>
        propsProxy.wrap { props =>
          filesDropZoneR.PropsVal(
            // Сборка функции доступа к FRK. Т.к. инстанс всегда разный,
            // FastEq проверяет только связанные с рендером стабильные данные, а сама модель props собирается здесь.
            mkActionF  = PictureFileChanged(_: Seq[dom.File], props.resKey): DAction,
            cssClasses = props.css,
          )
        }( filesDropZoneR.component(_)(inner) )(implicitly, filesDropZoneR.FilesDropZoneRFastEq)
      }
    }
    // Сравниваем только p.resKey и p.css:
    .build

  def _apply(propsValProxy: Props) = componentDrop( propsValProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}


case class ImgEditBtnPropsVal(
                               edge             : Option[(MJdEdgeId, MEdgeDataJs)],
                               resKey           : MFormResourceKey,
                               bgColor          : Option[MColorData]  = None,
                               css              : List[String]        = Nil,
                               size             : String              = Css.Size.M,
                               cropOnClick      : Option[ISize2di]    = None
                             )
object ImgEditBtnPropsVal {

  implicit object ImgEditBtnRPropsValFastEq extends FastEq[ImgEditBtnPropsVal] {
    override def eqv(a: ImgEditBtnPropsVal, b: ImgEditBtnPropsVal): Boolean = {
      implicitly[FastEq[Option[(MJdEdgeId, MEdgeDataJs)]]].eqv(a.edge, b.edge) &&
      ImgEditBtnRPropsVal4DropZoneFastEq.eqv(a, b) &&
      (a.bgColor ===* b.bgColor) &&
      (a.size ==* b.size) &&
      (a.cropOnClick ===* b.cropOnClick)
    }
  }

  /** Для componentDrop достаточно упрощённого сравнивания только некоторых полей. */
  object ImgEditBtnRPropsVal4DropZoneFastEq extends FastEq[ImgEditBtnPropsVal] {
    override def eqv(a: ImgEditBtnPropsVal, b: ImgEditBtnPropsVal): Boolean = {
      MFormResourceKey.MFormImgKeyFastEq.eqv(a.resKey, b.resKey) &&
      (a.css ===* b.css)
    }
  }

}

