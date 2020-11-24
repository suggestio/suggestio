package io.suggest.jd.render.v

import com.github.souporserious.react.measure.ContentRect
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.jd.MJdTagId
import io.suggest.jd.render.m._
import io.suggest.log.Log
import io.suggest.react.ReactDiodeUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */
final class JdR(
                 jdRrr                : JdRrr,
                 qdRrrHtml            : QdRrrHtml,
               )
  extends Log
{ jdR =>

  type Props_t = MJdArgs
  type Props = ModelProxy[Props_t]


  /** Обычная рендерилка без функций редактирования. */
  private object JdRrr extends jdRrr.Base {

    /** Реализация контента. */
    final class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends QdContentBase {

      override def _qdContentRrrHtml(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        qdRrrHtml
          .QdRrr( qdRrrHtml.RenderOnly, propsProxy )
          .render()
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      override def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          propsProxy: ModelProxy[MJdRrrProps] =>
            _qdBoundsMeasured( propsProxy, timeStampMs, cr )
        }
      }

      /** Фасад для рендера. */
      def render( propsProxy: ModelProxy[MJdRrrProps] ): VdomElement =
        _doRender( propsProxy )

    }
    /** Сборка react-компонента. def - т.к. в редакторе может не использоваться. */
    val qdContentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContentB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[QdContentB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    def mkQdContent(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      qdContentComp.withKey(key)(props)


    /** Реализация блока для обычного рендера. */
    final class BlockB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends BlockBase {

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderBlockTag(propsProxy)
      }

    }
    val blockComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[BlockB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    def mkBlock(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      blockComp.withKey(key)(props)


    /** Реализация документа для обычного рендера. */
    final class DocumentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends DocumentBase {

      def render(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        _renderGrid( propsProxy )
      }

    }
    // lazy, т.к. в выдаче компонент обычно не нужен - там многое вскрывается в GridCoreR.
    lazy val documentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[DocumentB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    /** Сборка инстанса компонента. Здесь можно навешивать hoc'и и прочее счастье. */
    override def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      documentComp.withKey(key)(props)


    // Обычный рендер, и RenderArgs управляется в GridCoreR или просто глобально пустой.
    override def getRenderArgs(jdtId: MJdTagId, jdArgs: Props_t): MJdRenderArgs =
      jdArgs.renderArgs

  }


  private def _apply(jdArgsProxy: Props) = JdRrr.anyTagComp( jdArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
