package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter._
import com.github.souporserious.react.measure.{ContentRect, Measure, MeasureChildrenArgs, MeasureProps}
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.css.Css
import io.suggest.grid.GridBuilderUtilJs
import io.suggest.jd.{MJdEdgeId, MJdTagId}
import io.suggest.jd.render.m._
import io.suggest.jd.tags._
import io.suggest.model.n2.edge.MPredicates
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactCommonUtil.Implicits._
import diode.data.PendingBase
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagOf, VdomElement}
import japgolly.univeq._
import org.scalajs.dom.html
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 19:00
  * Description: Ядро react-рендерера JSON-документов.
  */

class JdR(
           jdCssStatic        : JdCssStatic,
         )
  extends Log
{ jdR =>

  type Props_t = MJdArgs
  type Props = ModelProxy[Props_t]


  /** Для разделения edit и read-only режима используется этот трейт,
    * реализующий только базовый рендер с возможностью внутреннего расширения.
    */
  trait JdRrrBase {

    trait QdContentBase {

      def _qdContentRrrHtml(p: MJdRrrProps): QdRrrHtml = {
        QdRrrHtml(
          jdCssStatic = jdCssStatic,
          rrrProps    = p,
        )
      }

      def _renderQdContentTag(state: MJdRrrProps): TagOf[html.Element] = {
        import state.jdArgs.jdRuntime.jdCss

        val qdTag = state.subTree.rootLabel

        var contTagModsAcc = List[TagMod](
          _bgColorOpt( qdTag, state.jdArgs ),

          // Рендер qd-контента в html.
          _qdContentRrrHtml( state ).render(),
        )

        if (qdTag.props1.topLeft.nonEmpty) {
          // Обычный контент внутри блока.
          contTagModsAcc =
            // Поддержка перетаскивания внутри блока
            (jdCssStatic.absPosStyleAll: TagMod) ::
            // Поддержка абсолютного позиционирования внутри контейнера:
            (jdCss.absPosStyleF( state.tagId ): TagMod) ::
            contTagModsAcc
        } else {
          // Внеблоковый контент. Бывает, что высота уже измерена и не измерена. Отработать обе ситуации:
          contTagModsAcc ::= jdCssStatic.qdBl
          val qdBlOpt = state.jdArgs.jdRuntime.data.qdBlockLess.get( state.tagId )
          if ( qdBlOpt.exists(_.nonEmpty) )
            contTagModsAcc ::= jdCss.absPosStyleF( state.tagId )
        }

        // Вращение, если активно:
        for (rotateDeg <- qdTag.props1.rotateDeg)
          contTagModsAcc ::= state.jdArgs.jdRuntime.jdCss.rotateF( rotateDeg )

        // CSS-класс принудительной ширины, если задан.
        if (qdTag.props1.widthPx.nonEmpty)
          contTagModsAcc ::= jdCss.forcedWidthStyleF( state.tagId )

        // Стиль для теней
        if( qdTag.props1.textShadow.nonEmpty )
          contTagModsAcc ::= jdCss.contentShadowF( state.tagId )

        <.div( contTagModsAcc : _* )
      }

      def _doRender(state: MJdRrrProps): VdomElement = {
        // Если рендер ВНЕ блока, то нужно незаметно измерить высоту блока.
        val tag0 = _renderQdContentTag(state)
        if (
          state.parent
            .exists(_.name ==* MJdTagNames.STRIP)
        ) {
          // Рендер внутри блока, просто пропускаем контент на выход
          tag0
        } else {
          // Для наружной обёртки react-dnd требуется только нативный тег:
          val qdBlOpt = state.jdArgs
            .jdRuntime
            .data
            .qdBlockLess
            .get( state.tagId )

          <.div(
            // Сборка измерителя размеров тега:
            Measure {
              // Собираем callback реакции на ресайз: она должна слать таймштамп из Pot'а, чтобы контроллер мог разобрать,
              // актуален ли callback. Может быть неактуален, т.к. замер происходит в рамках requestAnimationFrame(), которая слишком асинхронна.
              val __onResizeF = ReactCommonUtil.cbFun1ToJsCb(
                blocklessQdContentBoundsMeasuredJdCb(
                  for {
                    qdBl <- qdBlOpt
                    if qdBl.isPending
                    qdBlPend = qdBl.asInstanceOf[PendingBase]
                  } yield {
                    qdBlPend.startTime
                  }
                )
              )

              // Локальная переменная для блокирования бесконечного вызова mkChildrenF().
              // Вызов args.measure() вызывает тело mkChildrenF() из requestAnimationFrame().
              // Без ручной переменной isReMeasured снаружи от mkChildrenF() рендер будет бесконечен после однократного вызова measure().
              var isReMeasured = false

              def __mkChildrenF(chArgs: MeasureChildrenArgs): raw.PropsChildren = {
                // Запустить повторное измерение, когда это требуется состоянием.
                if (
                  !isReMeasured && qdBlOpt.exists(_.isPending)
                ) {
                  isReMeasured = true
                  chArgs.measure()
                }

                tag0(
                  ^.genericRef := chArgs.measureRef,
                )
                  .rawElement
              }
              new MeasureProps {
                override val client = true
                override val bounds = true
                override val children = __mkChildrenF
                override val onResize = __onResizeF
              }
            }
          )
        }
      }

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback

      def _qdBoundsMeasured(mp: ModelProxy[MJdRrrProps], timeStampMs: Option[Long], cr: ContentRect): QdBoundsMeasured = {
        val m = mp.value
        QdBoundsMeasured(m.subTree.rootLabel, m.tagId, timeStampMs, cr)
      }

    }

    def mkQdContent(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    trait BlockBase {

      /** Доп.наполнение для тега фоновой картинки блока. */
      def _bgImgAddons(bgImgData: MJdEdgeId, edge: MEdgeDataJs, state: MJdRrrProps): TagMod = {
        // Просто заполнение всего блока картинкой. Т.к. фактический размер картинки отличается от размера блока
        // на px ratio, надо подогнать картинку по размерам:
        state.jdArgs.jdRuntime.jdCss.stripBgStyleF( state.tagId )
      }

      /** Доп.наполнение для sm-block div'a. */
      def _smBlockAddons(state: MJdRrrProps): TagMod =
        TagMod.empty

      /** Доп.наполнения для самого внешнего div'а. */
      def _outerContainerAddons(state: MJdRrrProps): TagMod =
        TagMod.empty


      def _renderBlockTag(propsProxy: ModelProxy[MJdRrrProps]): TagOf[html.Div] = {
        val state = propsProxy.value
        val s = state.subTree.rootLabel
        val C = state.jdArgs.jdRuntime.jdCss

        val isWide = s.props1.bm.hasExpandMode

        val bgColor = _bgColorOpt(s, state.jdArgs)

        val groupOutlineTm = state.jdArgs.renderArgs
          .groupOutLined
          .whenDefined { mcd =>
            TagMod(
              C.blockGroupOutline,
              ^.outlineColor := mcd.hexCode
            )
          }

        val bgImgOpt = for {
          bgImgData <- s.props1.bgImg
          edge      <- state.jdArgs.data.edges.get( bgImgData.edgeUid )
          if edge.jdEdge.predicate ==>> MPredicates.JdContent.Image
          bgImgSrc  <- edge.origImgSrcOpt
        } yield {
          <.img(
            ^.`class` := Css.Block.BG,
            ^.src := bgImgSrc,

            _bgImgAddons(bgImgData, edge, state),

            // В jdArgs может быть задан дополнительные модификации изображения, если selected tag.
            state.jdArgs.renderArgs.selJdtBgImgMod
              .filter(_ => state.isCurrentSelected)
              .whenDefined
          )
        }
        val bgImgTm = bgImgOpt.whenDefined

        val maybeSelAV = _outerContainerAddons(state)

        // Скрывать не-main-стрипы, если этого требует рендер.
        // Это касается только стрипов, у которых нет isMain = Some(true)
        val hideNonMainStrip = ReactCommonUtil.maybe(
          state.jdArgs.renderArgs.hideNonMainStrips &&
            !s.props1.isMain.getOrElseFalse
        ) {
          // Данный стип надо приглушить с помощью указанных css-стилей.
          ^.visibility.hidden
        }

        val smBlock = <.div(
          jdCssStatic.smBlockS,
          C.smBlock,
          C.bmStyleF( state.tagId ),

          if (isWide) {
            jdCssStatic.wideBlockStyle
          } else {
            TagMod(
              hideNonMainStrip,
              bgColor,
              maybeSelAV,
              groupOutlineTm
            )
          },

          _smBlockAddons(state),

          // Если задана фоновая картинка, от отрендерить её.
          bgImgTm.unless(isWide),

          renderChildren( propsProxy )
            .toVdomArray
        )

        if (isWide) {
          // Широкоформатное отображение, рендерим фон без ограничений блока:
          <.div(
            groupOutlineTm,
            hideNonMainStrip,
            bgColor,
            C.wideContStyleF(state.tagId),
            maybeSelAV,
            ^.`class` := Css.flat( Css.Overflow.HIDDEN, Css.Position.RELATIVE ),
            bgImgTm.when(isWide),
            smBlock
          )
        } else {
          // Обычное отображение, просто вернуть блок.
          smBlock
        }

      }

    }

    def mkBlock(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    /** Базовый трейт для рендера компонента документа. */
    trait DocumentBase {

      def _renderGrid(propsProxy: ModelProxy[MJdRrrProps]): VdomElement = {
        val state = propsProxy.value

        // Плитку отсюда полностью вынести не удалось.
        CSSGrid {
          GridBuilderUtilJs.mkCssGridArgs(
            gbRes = state.gridBuildRes.getOrElse {
              LOG.error( ErrorMsgs.GRID_BUILD_RES_MISSING, msg = state.tagId.toString )
              throw new IllegalArgumentException( ErrorMsgs.GRID_BUILD_RES_MISSING )
            },
            conf = state.jdArgs.conf,
            tagName = GridComponents.DIV,
          )
        } (
          renderChildrenWithId( propsProxy )
            // Тут тег-обёртка-костыль - что для CSSGrid нужен обязательно теги, а НЕ react-компоненты в children.
            .map { case (id, p) =>
              <.div(
                ^.key := id.toString,
                p
              )
            }: _*
        )
      }

    }

    /** Сборка инстанса компонента. Здесь можно навешивать hoc'и и прочее счастье. */
    def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement


    // -------------------------------------------------------------------------------------------------

    /**
      * Запуск рендеринга произвольных тегов.
      */
    // TODO parent может быть необязательным. Но это сейчас не востребовано, поэтому он обязательный
    def renderTag( proxy: ModelProxy[MJdRrrProps] ): VdomElement = {
      import MJdTagNames._
      val p = proxy.value
      p.subTree.rootLabel.name match {
        case QD_CONTENT                => mkQdContent( p.tagId.toString, proxy )
        case STRIP                     => mkBlock( p.tagId.toString, proxy )
        case DOCUMENT                  => mkDocument( p.tagId.toString, proxy )
        // Это пока не вызывается, т.к. QD_OP отрабатывается в QdRrrHtml.
        case other =>
          throw new UnsupportedOperationException( other.toString )
      }
    }


    /** Отрендерить дочерние элементы тега обычным методом.
      * @return Итератор отрендеренных vdom-узлов.
      */
    def renderChildrenWithId(proxy: ModelProxy[MJdRrrProps]): Stream[(MJdTagId, VdomNode)] = {
      val p = proxy.value
      val chs = p.subTree.subForest

      if (chs.nonEmpty) {
        val parent = p.subTree.rootLabel
        // Для qd-content-тегов надо указывать родительский тег.
        val parentSome = OptionUtil.maybe(parent.name ==* MJdTagNames.STRIP)( parent )
        chs
          .zipWithIndex
          .map { case (childJdTree, i) =>
            val ch = childJdTree.rootLabel
            val tagId = p.tagId.copy(
              selPathRev  = i :: p.tagId.selPathRev,
              blockExpand = if (ch.name ==* MJdTagNames.STRIP) {
                ch.props1.bm
                  .flatMap(_.expandMode)
              } else {
                p.tagId.blockExpand
              }
            )
            val p2 = p.copy(
              subTree = childJdTree,
              tagId   = tagId,
              parent  = parentSome,
            )
            // Вместо wrap используем прямой зум, чтобы избежать вызова цепочек функций, создающих множественные
            // инстансы одинаковых MJdRrrProps, которые будут удлиняться с каждым под-уровнем.
            val proxy2 = proxy.resetZoom( p2 )
            tagId -> renderTag( proxy2 )
          }
      } else {
        Stream.empty
      }
    }
    def renderChildren(proxy: ModelProxy[MJdRrrProps]): Stream[VdomNode] = {
      renderChildrenWithId(proxy)
        .map(_._2)
    }


    val anyTagComp = ScalaComponent
      .builder[ModelProxy[MJdArgs]]( jdR.getClass.getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .render_P { propsProxy =>
        propsProxy.wrap { jdArgs =>
          MJdRrrProps(
            subTree = jdArgs.data.doc.template,
            tagId   = jdArgs.data.doc.jdId,
            jdArgs  = jdArgs,
          )
        }( renderTag )(implicitly, MJdRrrProps.MJdRrrPropsFastEq)
      }
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdArgs.MJdArgsFastEq) )
      .build

  }


  /** Обычная рендерилка. */
  object JdRrr extends JdRrrBase {

    /** Реализация контента. */
    final class QdContentB($: BackendScope[ModelProxy[MJdRrrProps], MJdRrrProps]) extends QdContentBase {

      /** Реакция на получение информации о размерах внеблокового qd-контента. */
      override def blocklessQdContentBoundsMeasuredJdCb(timeStampMs: Option[Long])(cr: ContentRect): Callback = {
        ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
          propsProxy: ModelProxy[MJdRrrProps] =>
            _qdBoundsMeasured( propsProxy, timeStampMs, cr )
        }
      }

      /** Фасад для рендера. */
      def render(state: MJdRrrProps): VdomElement =
        _doRender(state)

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
    val documentComp = ScalaComponent
      .builder[ModelProxy[MJdRrrProps]]( classOf[BlockB].getSimpleName )
      .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
      .renderBackend[DocumentB]
      .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(MJdRrrProps.MJdRrrPropsFastEq) )
      .build
    /** Сборка инстанса компонента. Здесь можно навешивать hoc'и и прочее счастье. */
    override def mkDocument(key: Key, props: ModelProxy[MJdRrrProps]): VdomElement =
      documentComp.withKey(key)(props)

  }


  /** Рендер опционального цвета фона. */
  private def _bgColorOpt(jdTag: JdTag, jdArgs: MJdArgs): TagMod = {
    jdTag.props1.bgColor.whenDefined { mcd =>
      jdArgs.jdRuntime.jdCss.bgColorOptStyleF( mcd.hexCode )
    }
  }


  private def _apply(jdArgsProxy: Props) = JdRrr.anyTagComp( jdArgsProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
