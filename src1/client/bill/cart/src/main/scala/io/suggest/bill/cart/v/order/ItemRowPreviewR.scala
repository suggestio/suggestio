package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{MuiTableCell, MuiTableCellClasses, MuiTableCellProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.dev.MSzMults
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs}
import io.suggest.jd.render.m.MJdArgs.MJdArgsFastEq
import io.suggest.jd.render.v.{JdCss, JdR}
import io.suggest.jd.tags.JdTag
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.OptFastEq.Wrapped
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.18 16:57
  * Description: Обёртка над jd-рендером для рендера ячейки с превьюшкой карточки в таблице рендера.
  */
class ItemRowPreviewR(
                       jdR        : JdR,
                       orderCss   : OrderCss,
                     ) {

  /** Модель пропертисов компонента.
    *
    * @param jdArgs Данные для рендера шаблона.
    * @param jdRowSpan Сколько рядов по вертикали надо захватить?
    */
  case class PropsVal(
                       jdArgs     : MJdArgs,
                       jdRowSpan  : Int,
                     )
  implicit object ItemRowPreviewRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      // Инстанс MJdArgs может пересобираться наверху на каждый чих.
      MJdArgsFastEq.eqv( a.jdArgs, b.jdArgs ) &&
      (a.jdRowSpan ==* b.jdRowSpan)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { props =>
        // Рендер миниатюры карточки, если задана.
        // Ячейка с превьюшкой.
        val cssClasses = new MuiTableCellClasses {
          override val root = orderCss.ItemsTable.AdPreviewColumn.body.htmlClass
        }
        MuiTableCell(
          new MuiTableCellProps {
            // Передаём rowspan напрямую в атрибуты td:
            val rowSpan = props.jdRowSpan.toString
            override val classes = cssClasses
          }
        )(
          propsProxy.wrap(_ => props.jdArgs)(jdR.apply)
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

  def apply( propsValOptProxy: Props ) = component( propsValOptProxy )

}


object ItemRowPreviewR {

  /** Инстанс jd-conf един среди всего компонента. */
  val JD_CONF = MJdConf(
    isEdit = false,
    szMult = MSzMults.`0.25`,
    gridColumnsCount = 2
  )

  /** Сборка пустого стиля для jd-рендера. */
  def mkJdCss(templates: Seq[Tree[JdTag]] = Nil): JdCss =
    JdCss( MJdCssArgs(templates, JD_CONF, quirks = false) )

}
