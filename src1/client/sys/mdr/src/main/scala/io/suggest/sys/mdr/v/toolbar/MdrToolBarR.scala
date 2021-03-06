package io.suggest.sys.mdr.v.toolbar

import com.materialui.{MuiChip, MuiChipProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants._
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.spa.OptFastEq
import io.suggest.sys.mdr.{MMdrQueueReport, SysMdrConst}
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.18 18:13
  * Description: Тулбар с кнопками панели управления системой модерации.
  */
class MdrToolBarR(
                   mdrTbStepBtnR      : MdrTbStepBtnR,
                   mdrTbAnchorBtnR    : MdrTbAnchorBtnR,
                 ) {

  case class PropsVal(
                       nodePending      : Boolean,
                       nodeOffset       : Int,
                       nodeIdOpt        : Option[String],
                       ntypeOpt         : Option[MNodeType],
                       queueReportOpt   : Option[MMdrQueueReport],
                       errorsCount      : Int,
                     )
  implicit object MdrControlPanelRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      // TODO Заменить на UseValueEq?
      (a.nodePending ==* b.nodePending) &&
      (a.nodeOffset ==* b.nodeOffset) &&
      (a.nodeIdOpt ==* b.nodeIdOpt) &&
      (a.ntypeOpt ==* b.ntypeOpt) &&
      OptFastEq.Plain.eqv(a.queueReportOpt, b.queueReportOpt) &&
      (a.errorsCount ==* b.errorsCount)
    }
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      def __stepBtn(btnProps: mdrTbStepBtnR.Props_t) =
        propsProxy.wrap(_ => btnProps)(mdrTbStepBtnR.apply)

      def __anchorBtn( btnProps: mdrTbAnchorBtnR.Props_t ) =
        propsProxy.wrap(_ => btnProps)(mdrTbAnchorBtnR.apply)

      val canNotGoToPrevious = props.nodeOffset - props.errorsCount <= 0

      val A = mdrTbAnchorBtnR.PropsVal
      val S = mdrTbStepBtnR.PropsVal

      <.span(

        MuiChip(
          new MuiChipProps {

            override val variant = MuiChip.Variant.OUTLINED
            override val label: js.UndefOr[React.Node] = {
              <.span(
                (props.nodeOffset + 1 + props.errorsCount).toString,
                NBSP_STR, SLASH, NBSP_STR,
                props.queueReportOpt.fold(QUESTION_MARK) { _.toHumanReadableString }
              )
                .rawNode
            }
          }
        ),

        NBSP_STR,

        __stepBtn( S.ToBeginning(canNotGoToPrevious, -props.nodeOffset) ),
        __stepBtn( S.PreviousNode(canNotGoToPrevious) ),
        __stepBtn( S.Refresh(props.nodePending) ),
        __stepBtn( S.NextNode(
          isDisabled = {
            props.nodeIdOpt.isEmpty ||
            !props.queueReportOpt.exists { qr =>
              !qr.hasMore &&
              qr.len - 1 > props.nodeOffset &&
              props.nodeOffset < SysMdrConst.MAX_OFFSET
            }
          },
        )),
        __stepBtn( S.ToEnd(
          offsetDelta = for {
            queueReport <- props.queueReportOpt
            len2Offset = queueReport.len - 1
            if (len2Offset > props.nodeOffset) &&
               (props.nodeOffset < SysMdrConst.MAX_OFFSET)
          } yield {
            Math.min( len2Offset - props.nodeOffset, SysMdrConst.MAX_OFFSET )
          }
        )),

        // Если есть узел, то надо ссылку на редактор сделать:
        props.nodeIdOpt.whenDefined { nodeId =>
          <.span(
            PIPE,

            // Ссылка на sys-страницу узла:
            __anchorBtn( A.SysNodeShow(nodeId, props.nodePending) ),

            // Ссылка на лк-редактор узла в зависимости от типа узла.
            props
              .ntypeOpt
              .collect {
                case MNodeTypes.Ad      => A.LkAdEdit _
                case MNodeTypes.AdnNode => A.LkAdnEdit _
              }
              .whenDefined { propsF =>
                __anchorBtn( propsF(nodeId, props.nodePending) )
              }
          )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
