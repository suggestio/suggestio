package io.suggest.adn.edit.v

import com.github.react.dnd.backend.html5.Html5Backend
import com.github.react.dnd.{DndProvider, DndProviderProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.adn.edit.m._
import io.suggest.color.{IColorPickerMarker, MColorData, MColorType, MColorTypes, MColors}
import io.suggest.css.Css
import japgolly.scalajs.react._
import japgolly.univeq._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import io.suggest.css.ScalaCssDefaults._
import io.suggest.form.{MFormResourceKey, MFrkTypes}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdEdgeId
import io.suggest.lk.m.DocBodyClick
import io.suggest.lk.r.{PropTableR, UploadStatusR}
import io.suggest.lk.r.color.{ColorBtnR, ColorPickerR}
import io.suggest.lk.r.img.{ImgEditBtnPropsVal, ImgEditBtnR}
import io.suggest.msg.Messages
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.spa.OptFastEq
import io.suggest.spa.FastEqUtil
import io.suggest.up.MFileUploadS
import monocle.Lens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 19:33
  * Description: Корневой react-компонент формы редактирования ADN-узла.
  */
class LkAdnEditFormR(
                      val oneRowR         : OneRowR,
                      val colorPickerR    : ColorPickerR,
                      val colorBtnR       : ColorBtnR,
                      imgEditBtnR         : ImgEditBtnR,
                      val uploadStatusR   : UploadStatusR,
                      val nodeGalleryR    : NodeGalleryR,
                      lkAdEditCss         : LkAdnEditCss,
                      wcFgContR           : WcFgContR,
                    ) {

  type Props = ModelProxy[MLkAdnEditRoot]


  /** Состояние картинок. */
  case class ImgState(
                       uploadStatusC    : ReactConnectProxy[uploadStatusR.Props_t]
                     )

  /** Состояние компонента: все react-коннекшены. */
  case class State(
                    nameC           : ReactConnectProxy[oneRowR.ValueVal],
                    townC           : ReactConnectProxy[oneRowR.ValueVal],
                    addressC        : ReactConnectProxy[oneRowR.ValueVal],
                    siteUrlC        : ReactConnectProxy[oneRowR.ValueVal],
                    infoAboutC      : ReactConnectProxy[oneRowR.ValueVal],
                    humanTrafficC   : ReactConnectProxy[oneRowR.ValueVal],
                    audienceDescrC  : ReactConnectProxy[oneRowR.ValueVal],
                    bgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    fgColorC        : ReactConnectProxy[colorBtnR.Props_t],
                    colorPickerC    : ReactConnectProxy[colorPickerR.Props_t],
                    logo            : ImgState,
                    wcFg            : ImgState,
                    galImgs         : ReactConnectProxy[nodeGalleryR.Props_t]
                  )

  private def __colorTd(cssClasses: List[String])(content: TagMod): VdomElement = {
    <.td(
      ^.`class` := Css.flat1( Css.Table.Td.TD :: cssClasses ),
      content
    )
  }


  class Backend($: BackendScope[Props, State]) {

    private def onNameChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetName(e.target.value) )
    }

    private def onTownChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetTown(e.target.value) )
    }

    private def onAddressChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetAddress(e.target.value) )
    }

    private def onSiteUrlChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetSiteUrl(e.target.value) )
    }

    private def onInfoAboutProductsChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetInfo(e.target.value) )
    }

    private def onHumanTrafficChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetHumanTraffic(e.target.value) )
    }

    private def onAudienceDescrChange(e: ReactEventFromInput): Callback = {
      dispatchOnProxyScopeCB( $, SetAudienceDescr(e.target.value) )
    }

    private def onDocumentClick: Callback = {
      dispatchOnProxyScopeCB( $, DocBodyClick )
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      val delimHr = <.hr(
        ^.`class` := Css.flat( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT )
      )

      val paddingS = Css.Lk.Paddings.S

      val content = <.div(
        ^.onClick --> onDocumentClick,

        // Отрендерить доп.стили
        <.styleTag(
          lkAdEditCss.render[String]
        ),

        <.div(
          lkAdEditCss.logoBar,

          propsProxy.wrap { props =>
            val cssSizeL = Css.Size.L
            ImgEditBtnPropsVal(
              edge     = __getImgEdgeOpt(props)(MAdnResView.logo),
              resKey   = props.node.logoFrk,
              bgColor = props.node.meta.colors.bg,
              size    = cssSizeL
            )
          }( imgEditBtnR.apply )(implicitly, ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ),

          s.logo.uploadStatusC { uploadStatusR.apply },
        ),

        <.div(
          lkAdEditCss.infoBar,

          delimHr,

          // Поле названия узла:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Name`, onNameChange, s.nameC, isRequired = true) ),

          // Поле города узла:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Town`, onTownChange, s.townC ) ),

          // Поле адреса:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Address`, onAddressChange, s.addressC, isTextArea = true) ),

          // Поле URL сайта:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Site`, onSiteUrlChange, s.siteUrlC )),

          // Инфа о товарах и услугах:
          oneRowR( oneRowR.PropsVal( MsgCodes.`Info.about.prods.and.svcs`, onInfoAboutProductsChange, s.infoAboutC, isTextArea = true )),


          delimHr,

          // Поле задания человеческого трафика.
          oneRowR( oneRowR.PropsVal( MsgCodes.`Daily.people.traffic`, onHumanTrafficChange, s.humanTrafficC ) ),

          // Поле описания аудитории.
          oneRowR( oneRowR.PropsVal( MsgCodes.`Audience.descr`, onAudienceDescrChange, s.audienceDescrC, isTextArea = true ) ),


          delimHr,

          // Экран приветствия
          wcFgContR(

            propsProxy.wrap { props =>
              ImgEditBtnPropsVal(
                edge = __getImgEdgeOpt(props)( MAdnResView.wcFg ),
                resKey = props.node.wcFgFrk,
              )
            }( imgEditBtnR.apply )(implicitly, ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ),

            // Картинка приветствия
            s.wcFg.uploadStatusC { uploadStatusR.apply }

          ),


          delimHr,

          // Галерея фотографий магазина:
          s.galImgs { nodeGalleryR.apply },


          delimHr,

          // Цвета фона и контента
          PropTableR.Outer(
            <.tr(
              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                Messages( MsgCodes.`Bg.color` )
              ),
              __colorTd( paddingS :: Nil )(
                s.bgColorC { colorBtnR.apply }
              ),

              // Вертикальная линия-разделитель:
              __colorTd( paddingS :: Nil )(
                <.div(
                  lkAdEditCss.colorTdVerticalHr
                )
              ),

              __colorTd( Css.Lk.Adn.Edit.Colors.COLOR_TITLE :: Css.PropTable.TD_NAME :: Nil )(
                Messages( MsgCodes.`Fg.color.of.sc.hint` )
              ),
              __colorTd( paddingS :: Nil )(
                s.fgColorC { colorBtnR.apply }
              )
            )
          )

        ),

        s.colorPickerC { colorPickerR.apply }

      )

      DndProvider.component(
        new DndProviderProps {
          override val backend = Html5Backend
        }
      )(content)
    }

  }


  private def __getImgEdgeOpt(mroot: MLkAdnEditRoot)(lens: Lens[MAdnResView, Option[MJdEdgeId]]): Option[(MJdEdgeId, MEdgeDataJs)] = {
    for {
      ei <- lens.get(mroot.node.resView)
      edge <- mroot.node.edges.get( ei.edgeUid )
    } yield {
      (ei, edge)
    }
  }

  private def __getImgUploadOpt(mroot: MLkAdnEditRoot)(lens: Lens[MAdnResView, Option[MJdEdgeId]]): Option[MFileUploadS] = {
    __getImgEdgeOpt(mroot)(lens)
      .flatMap(_._2.upload)
  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps { propsProxy =>
      // Сборка коннекшена до цвета:
      val colorBtnROptFastEq = OptFastEq.Wrapped( colorBtnR.ColorBtnRPropsValFastEq )
      def __getColorBtnC(colorType: MColorType with IColorPickerMarker): ReactConnectProxy[colorBtnR.Props_t] = {
        val colorTypeSome = Some(colorType)
        lazy val mcdDflt = MColorData( colorType.scDefaultHex )
        propsProxy.connect { props =>
          val mcd = MColors
            .ofType( colorType )
            .get( props.node.meta.colors )
            .getOrElse( mcdDflt )
          val propsVal = colorBtnR.PropsVal(
            color     = mcd,
            marker    = colorTypeSome
          )
          Some( propsVal ): colorBtnR.Props_t
        }( colorBtnROptFastEq )
      }

      val emptyStr = ""
      def emptyStrF: String = emptyStr

      val uploadStatusOptFastEq = OptFastEq.Wrapped( MFileUploadS.MFileUploadSFastEq )

      State(
        nameC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.name getOrElse emptyStrF,
            error = props.node.errors.name,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        townC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.address.town getOrElse emptyStrF,
            error = props.node.errors.address.town,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        addressC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.address.address getOrElse emptyStrF,
            error = props.node.errors.address.address,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        siteUrlC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.business.siteUrl getOrElse emptyStrF,
            error = props.node.errors.business.siteUrl,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        infoAboutC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.business.info getOrElse emptyStrF,
            error = props.node.errors.business.info,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        humanTrafficC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.business.humanTraffic getOrElse emptyStrF,
            error = props.node.errors.business.humanTraffic,
          )
        }( oneRowR.OneRowRValueValFastEq ),
        audienceDescrC = propsProxy.connect { props =>
          oneRowR.ValueVal(
            value = props.node.meta.business.audienceDescr getOrElse emptyStrF,
            error = props.node.errors.business.audienceDescr,
          )
        }( oneRowR.OneRowRValueValFastEq ),

        bgColorC = __getColorBtnC( MColorTypes.Bg ),
        fgColorC = __getColorBtnC( MColorTypes.Fg ),
        colorPickerC = propsProxy.connect { props =>
          for {
            cps <- props.internals.colorState.picker
            marker0 <- cps.marker
            marker = marker0.asInstanceOf[MColorType]
            mcd = MColors
              .ofType( marker )
              .get( props.node.meta.colors )
              .getOrElse( MColorData(marker.scDefaultHex) )
          } yield {
            colorPickerR.PropsVal(
              color         = mcd,
              colorPresets  = props.internals.colorState.colorPresets.colors,
              topLeftPx     = Some(
                cps.shownAt.copy(
                  x = cps.shownAt.x - (if (marker ==* MColorTypes.Fg) 220 else 0),
                  y = cps.shownAt.y - 260
                )
              ),
              cssClass      = Some( lkAdEditCss.colorPicker.htmlClass )
            )
          }
        }( OptFastEq.Wrapped(colorPickerR.ColorPickerPropsValFastEq) ),

        logo = ImgState(
          uploadStatusC = propsProxy.connect { props =>
            __getImgUploadOpt(props)(MAdnResView.logo)
          }( uploadStatusOptFastEq )
        )
        ,

        wcFg = ImgState(
          uploadStatusC = propsProxy.connect { props =>
            __getImgUploadOpt(props)(MAdnResView.wcFg)
          }( uploadStatusOptFastEq )
        ),

        galImgs = {
          val cropOnClickSome = Some( NodeEditConstants.Gallery.WH_PX )
          propsProxy.connect { props =>
            if (props.node.resView.galImgs.isEmpty) {
              Nil
            } else {
              (for {
                galImg <- props.node.resView.galImgs.iterator
                edge <- props.node.edges
                  .get( galImg.edgeUid )
                  .iterator
              } yield {
                nodeGalleryR.PropsValEl(
                  editBtn = ImgEditBtnPropsVal(
                    edge   = Some( (galImg, edge) ),
                    resKey = MFormResourceKey(
                      frkType  = MFrkTypes.somes.GalImgSome,
                      edgeUid  = Some( galImg.edgeUid )
                    ),
                    bgColor = None,
                    css     = nodeGalleryR.imgsRowContCss,
                    cropOnClick = cropOnClickSome
                  ),
                  uploadStatus = edge.fileJs
                    .map(_.upload)
                )
              })
                .toSeq
            }
          }( FastEqUtil.CollFastEq[nodeGalleryR.PropsValEl, Seq]( nodeGalleryR.NodeGalleryRPropsValElFastEq ) )
        }

      )
    }
    .renderBackend[Backend]
    .build

}
