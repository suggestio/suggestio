package io.suggest.jd.render.v

import com.github.souporserious.react.measure.ContentRect
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.jd.MJdTagId
import io.suggest.jd.render.m._
import io.suggest.log.Log
import io.suggest.react.ReactDiodeUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

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

    final class QdContent($: BackendScope[ModelProxy[MJdRrrProps], Unit]) extends super.QdContentBase {
      override def qdRrr = qdRrrHtml.RenderOnly

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      override def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          propsProxy: ModelProxy[MJdRrrProps] =>
            _qdBoundsMeasured( propsProxy, timeStampMs, cr )
        }
      }

    }
    val QdContent = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContent].getSimpleName )
      //.initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[QdContent]
      //.configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( MJdRrrProps.MJdRrrPropsFastEq ) )
      .build


    /** Реализация контента. */
    final class QdContainer($: BackendScope[ModelProxy[MJdRrrProps], Unit]) extends super.QdContainerBase {

      /** Фасад для рендера. */
      def render( propsProxy: ModelProxy[MJdRrrProps], children: PropsChildren ): VdomElement =
        _doRender( propsProxy, children )

    }
    /** Сборка react-компонента. def - т.к. в редакторе может не использоваться. */
    val QdContainer = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[QdContainer].getSimpleName )
      .renderBackendWithChildren[QdContainer]
      .build


    /** Объединяющий компонент для qd-частей с единой проверкой shouldComponentUpdate(). */
    val QdAll = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( "Qd" )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .render_P { props =>
        QdContainer(props)(
          QdContent( props )
        )
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build

    override def mkQdContainer(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      QdAll.withKey( key )(props)


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
