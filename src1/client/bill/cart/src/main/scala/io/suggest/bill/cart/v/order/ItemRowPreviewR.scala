package io.suggest.bill.cart.v.order

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
                     ) {

  // TODO Основной смысл компонента замёржился в другие компоненты. Заинлайнить вызов JdR прямо в OrderR?

  /** Модель пропертисов компонента.
    *
    * @param jdArgs Данные для рендера шаблона.
    * @param jdRowSpan Сколько рядов по вертикали надо захватить?
    */
  case class PropsVal(
                       jdArgs     : MJdArgs
                     )
  implicit object ItemRowPreviewRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      // Инстанс MJdArgs может пересобираться наверху на каждый чих.
      MJdArgsFastEq.eqv( a.jdArgs, b.jdArgs )
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { props =>
        propsProxy.wrap(_ => props.jdArgs)(jdR.apply)
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
