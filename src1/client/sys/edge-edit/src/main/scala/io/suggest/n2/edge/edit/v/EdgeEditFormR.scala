package io.suggest.n2.edge.edit.v

import com.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiPaper}
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import scalacss.ScalaCssReact._
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.lk.m.MErrorPopupS
import io.suggest.n2.edge.edit.m.{MDeleteDiaS, MEdgeEditRoot}
import io.suggest.n2.edge.edit.v.inputs.act.{DeleteBtnR, DeleteDiaR, ErrorDiaR, FileExistDiaR, SaveBtnR}
import io.suggest.n2.edge.edit.v.inputs.info.{InfoFlagR, InfoTextNiR}
import io.suggest.n2.edge.edit.v.inputs.media.MediaR
import io.suggest.n2.edge.edit.v.inputs.{NodeIdsR, OrderR, PredicateR}
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 10:43
  * Description: Компонент формы заливки файла.
  */
class EdgeEditFormR(
                     predicateEditR       : PredicateR,
                     nodeIdsR             : NodeIdsR,
                     infoFlagR            : InfoFlagR,
                     orderR               : OrderR,
                     infoTextNiR          : InfoTextNiR,
                     deleteBtnR           : DeleteBtnR,
                     deleteDiaR           : DeleteDiaR,
                     saveBtnR             : SaveBtnR,
                     mediaR               : MediaR,
                     errorDiaR            : ErrorDiaR,
                     fileExistsDiaR       : FileExistDiaR,
                     crCtxProv            : React.Context[MCommonReactCtx],
                   ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      val css = p.wrap(_ => EdgeEditCss)( CssR.apply )

      MuiPaper()(
        crCtxProv.provide( MCommonReactCtx.default )(
          <.div(
            MuiFormControl(
              new MuiFormControlProps {
                override val component = js.defined( <.fieldset.name )
              }
            )(
              css,

              MuiFormGroup()(

                // Предикат
                p.wrap( _.edge.predicate )( predicateEditR.component.apply ),

                // id узлов
                p.wrap( _.edit.nodeIds )( nodeIdsR.component.apply ),

                // порядковые номера эджей
                p.wrap( _.edge.order )( orderR.component.apply ),

                // legacy-флаг эджа
                MuiPaper()(
                  p.wrap( _.edge.info.flag )( infoFlagR.component.apply ),
                  p.wrap( _.edge.info.textNi )( infoTextNiR.component.apply ),
                ),

                // edge media
                p.wrap { m =>
                  mediaR.PropsVal(
                    media     = m.edge.media,
                    uploadReq = m.edit.upload,
                    edgeIdQs  = Some(m.conf),
                  )
                }( mediaR.component.apply )( implicitly, mediaR.MediaRPropsValFeq ),

                <.div(
                  EdgeEditCss.btnsCont,

                  // Кнопка удаления
                  p.wrap { m =>
                    OptionUtil.SomeBool( !m.edit.deleteDia.opened )
                  }( deleteBtnR.component.apply ),

                  // Кнопка сохранения
                  p.wrap { m =>
                    OptionUtil.SomeBool( m.edit.saveReq.isPending )
                  }( saveBtnR.component.apply ),
                ),

              ),

            ),

            // Диалог подтверждения удаления.
            p.wrap( _.edit.deleteDia )( deleteDiaR.component.apply )( implicitly, MDeleteDiaS.MDeleteDiaSFastEq ),

            // Диалог какой-то ошибки.
            p.wrap( _.edit.errorDia )( errorDiaR.component.apply )( implicitly, OptFastEq.Wrapped(MErrorPopupS.MErrorPopupSFastEq) ),

            // Диалог дубликата файла.
            p.wrap( _.edit.fileExistNodeId )( fileExistsDiaR.component.apply ),

          )
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
